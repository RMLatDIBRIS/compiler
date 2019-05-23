package rml.ast

import prolog.ast.*

// translates from RML AST to Prolog AST

fun toProlog(spec: Specification): LogicProgram {
    // predicates to be exported
    val traceExpressionIndicator = PredicateIndicatorTerm("trace_expression", 2)
    val matchIndicator = PredicateIndicatorTerm("match", 2)
    val exportedPredicates = ListTerm(listOf(traceExpressionIndicator, matchIndicator))

    // export as a module
    val moduleDeclaration = Directive(Atom("module", ConstantTerm("spec"), exportedPredicates))

    // import deep_subdict
    val deepSubdictImport = Directive(Atom("use_module",
            FunctionTerm("monitor", ConstantTerm("deep_subdict"))))

    // generate a match clause for each event type pattern
    val matchClauses = spec.evtypeDecls.map(::toProlog).flatten()
    val anyMatchClause = Clause(Atom("match", VarTerm("_"), FunctionTerm("any")))
    val traceExpClause = toProlog(spec.traceExpDecls)

    // spread operator can only be applied to arrays
    return LogicProgram(listOf(moduleDeclaration, deepSubdictImport),
            matchClauses + anyMatchClause + traceExpClause)
}

// build a match clause for each pattern if positive, or just one if negated
fun toProlog(evtypeDecl: EvtypeDecl): List<Clause> {
    // use an underscore, RML IDs cannot start with it
    // but not followed by an uppercase letter, that's for singleton variables...
    val eventDict = VarTerm("_event")

    // unfold patterns and collect atoms
    val patternAtoms: List<Atom> = when (evtypeDecl) {
        is DirectEvtypeDecl -> evtypeDecl.patternValue.unfoldOrPatterns().map {
            Atom("deep_subdict", toProlog(it, isMatchClause = true), eventDict)
        }
        is DerivedEvtypeDecl -> evtypeDecl.parents.map {
            Atom("match", eventDict, toProlog(it, isMatchClause = true))
        }
    }

    // match clause head is always the same
    val head = Atom("match", eventDict, toProlog(evtypeDecl.evtype, isMatchClause = true))

    // compile guards to atoms
    val guardAtom: Atom? = evtypeDecl.guard?.let { asAtom(toProlog(it, false) as FunctionTerm) }
    // as a list
    val guardAtoms: List<Atom> = if (guardAtom != null) listOf(guardAtom) else emptyList()

    val body: List<Atom> = patternAtoms + guardAtoms

    // if it's negated just make one match clause, applying negation to all body atoms
    if (evtypeDecl.negated) return listOf(Clause(
            head,
            body.map(Atom::negate)
    ))

    // if it's positive produce a match clause for each pattern, but replicate guard atoms
    return patternAtoms.map { Clause(head, listOf(it) + guardAtoms) }
}

fun asAtom(term: FunctionTerm) = Atom(Atom.PredicateSymbol(term.functionSymbol.name), false, term.args)

fun toProlog(dataValue: DataValue, isMatchClause: Boolean): PrologTerm = when (dataValue) {
    is ObjectValue -> DictionaryTerm.from(
            dataValue.fields.map { Pair(it.key.name, toProlog(it.value, isMatchClause)) }.toMap())
    is ListValue -> ListTerm(
            dataValue.values.map { toProlog(it, isMatchClause) },
            if (dataValue.hasEllipsis) VarTerm("_") else null)
    is SimpleValue -> toProlog(dataValue, isMatchClause)
    is OrPatternValue -> throw Exception("internal error: or-patterns should be unfolded by now")
}

fun toProlog(declarations: List<TraceExpDecl>): Clause {
    val head = Atom("trace_expression", ConstantTerm("Main"), VarTerm("Main"))
    val body = declarations.map(::toProlog)
    return Clause(head, body)
}

// output T = trace-expression
fun toProlog(declaration: TraceExpDecl): Atom {
    val varTerm = VarTerm(declaration.id.name)
    val paramTerms = declaration.vars.map { ConstantTerm(it.name) }
    var body = toProlog(declaration.traceExp)

    if (declaration.vars.isNotEmpty())
        body = FunctionTerm("gen", ListTerm(paramTerms), body)

    return Atom("=", varTerm, body)
}

fun toProlog(traceExp: TraceExp): PrologTerm {
    val isMatchClause = false
    return when(traceExp) {
        EmptyTraceExp -> FunctionTerm("eps")
        NoneTraceExp -> IntTerm(0)
        AllTraceExp -> IntTerm(1)
        is ClosureTraceExp -> FunctionTerm("clos", toProlog(traceExp.exp))
        is BlockTraceExp -> toProlog(traceExp)
        is TraceExpVar -> toProlog(traceExp)
        is EventTypeTraceExp -> FunctionTerm(":",
                toProlog(traceExp.eventType, isMatchClause),
                toProlog(EmptyTraceExp))
        is ConcatTraceExp -> toProlog(traceExp, "*")
        is AndTraceExp -> toProlog(traceExp, "/\\")
        is OrTraceExp -> toProlog(traceExp, "\\/")
        is ShuffleTraceExp -> toProlog(traceExp, "|")
        is FilterTraceExp -> FunctionTerm(";",
                FunctionTerm(">>",
                        toProlog(traceExp.evtype, isMatchClause), toProlog(traceExp.leftExp)),
                toProlog(traceExp.rightExp))
        is CondFilterTraceExp -> FunctionTerm(";",
                FunctionTerm(">",
                        toProlog(traceExp.evtype, isMatchClause), toProlog(traceExp.leftExp)),
                toProlog(traceExp.rightExp))
        is StarTraceExp -> FunctionTerm("star", toProlog(traceExp.exp))
        is PlusTraceExp -> FunctionTerm("plus", toProlog(traceExp.exp))
        is OptionalTraceExp -> FunctionTerm("optional", toProlog(traceExp.exp))
        is IfElseTraceExp -> FunctionTerm("guarded",
                toProlog(traceExp.condition),
                toProlog(traceExp.thenTraceExp),
                toProlog(traceExp.elseTraceExp))
        is EventTypeWithTraceExp -> FunctionTerm("with",
                toProlog(traceExp.eventType, isMatchClause),
                toProlog(EmptyTraceExp),
                toProlog(traceExp.exp))
    }
}

fun toProlog(block: BlockTraceExp): PrologTerm {
    // handle one variable at a time
    val firstVar = block.declaredVars.first()
    val otherVars = block.declaredVars.drop(1)
    val innerBlock: PrologTerm =
            if (otherVars.isEmpty()) toProlog(block.traceExp)
            else toProlog(BlockTraceExp(otherVars, block.traceExp))
    return FunctionTerm("var", FunctionTerm(firstVar.name), innerBlock)
}

fun toProlog(eventType: EventType, isMatchClause: Boolean) = FunctionTerm(
        eventType.id.name,
        eventType.dataValues.map { toProlog(it, isMatchClause) }.toList())

fun toProlog(traceExp: BinaryTraceExp, opSymbol: String) = FunctionTerm(
        opSymbol,
        toProlog(traceExp.left),
        toProlog(traceExp.right))

fun toProlog(traceExp: TraceExpVar): PrologTerm {
    val variable = VarTerm(traceExp.id.name)

    if (traceExp.genericArgs.isEmpty())
        return variable

    // convert all expressions
    val exps: List<PrologTerm> = traceExp.genericArgs.map { toProlog(it)}

    return FunctionTerm("app", variable, ListTerm(exps))
}

fun toProlog(exp: Exp, convertVars: Boolean = true): PrologTerm = when (exp) {
    is BoolExp -> ConstantTerm(exp.boolean.toString())
    is IntExp -> IntTerm(exp.int)
    is FloatExp -> FloatTerm(exp.double)
    is VarExp ->
        if (convertVars) FunctionTerm("var", ConstantTerm(exp.varId.name))
        else VarTerm(exp.varId.name.capitalize())
    is SumExp -> FunctionTerm("+", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is SubExp -> FunctionTerm("-", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is MulExp -> FunctionTerm("*", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is DivExp -> FunctionTerm("/", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is LessThanExp -> FunctionTerm("<", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is LessThanEqExp -> FunctionTerm("=<", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is GreaterThanExp -> FunctionTerm(">", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is GreaterThanEqExp -> FunctionTerm(">=", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is EqualToExp -> FunctionTerm("=:=", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is AssignExp -> FunctionTerm("=", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    // remember to use ',', otherwise it fucks up Prolog syntax
    is AndExp -> FunctionTerm("','", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is OrExp -> FunctionTerm(";", toProlog(exp.left, convertVars), toProlog(exp.right, convertVars))
    is UnaryMinusExp -> FunctionTerm("-", toProlog(exp.exp, convertVars))
}

// isMatchClause true when generating match clauses
fun toProlog(value: SimpleValue, isMatchClause: Boolean): PrologTerm = when(value) {
    is VarValue ->
        if (isMatchClause) VarTerm(toValidPrologVarName(value.id.name))
        else FunctionTerm("var", FunctionTerm(value.id.name))
    is IntValue -> IntTerm(value.number)
    is BooleanValue -> ConstantTerm(value.boolean.toString())
    is StringValue -> StringTerm(value.string)
    is ListSimpleValue -> ListTerm(
            value.values.map { toProlog(it, isMatchClause) },
            if (value.hasEllipsis) VarTerm("_") else null)
    is UnusedValue -> VarTerm("_")
}

fun toValidPrologVarName(string: String): String {
    if (string.startsWith("_"))
        return string

    val first = string.first()
    if (first.isLetter())
        return string.replaceFirst(first, first.toUpperCase())

    throw Exception("invalid Prolog variable name, can't fix")
}