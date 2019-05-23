package rml.parser

import rml.ast.*

fun buildSpecificationAst(ctx: rmlParser.SpecContext): Specification {
    val texpDecls = ctx.texpDecl().map(::buildDeclarationAst).toList()
    val evtypeDecls = ctx.evtypeDecl().map { it.accept(EvtypeDeclAstBuilder) }.toList()
    return Specification(evtypeDecls, texpDecls)
}

fun buildEventTypeAst(ctx: rmlParser.EvtypeContext) = EventType(
        ctx.LOWERCASE_ID().text,
        ctx.simpleValues()?.simpleValue()?.map { it.accept(SimpleValueAstBuilder) }?.toList()
                ?: emptyList()
)

fun buildFieldAst(ctx: rmlParser.FieldContext): ObjectValue.Field =
        ObjectValue.Field(ctx.LOWERCASE_ID().text, ctx.value().accept(DataValueAstBuilder))

fun buildDeclarationAst(ctx: rmlParser.TexpDeclContext) = TraceExpDecl(
                TraceExpId(ctx.UPPERCASE_ID().text),
                visitVarsAux(ctx.vars()),
                ctx.texp().accept(TraceExpAstBuilder)
        )

// use different visitors because the result types are not the same

object EvtypeDeclAstBuilder: rmlBaseVisitor<EvtypeDecl>() {
    override fun visitDirectEvtypeDecl(ctx: rmlParser.DirectEvtypeDeclContext?) = DirectEvtypeDecl(
            buildEventTypeAst(ctx!!.evtype()),
            ctx.value().accept(DataValueAstBuilder),
            ctx.NOT() != null,
            ctx.exp()?.accept(ExpBuilder))

    override fun visitDerivedEvtypeDecl(ctx: rmlParser.DerivedEvtypeDeclContext?) = DerivedEvtypeDecl(
            buildEventTypeAst(ctx!!.evtype().first()),
            ctx.evtype().drop(1).map(::buildEventTypeAst).toList(),
            ctx.NOT() != null,
            ctx.exp()?.accept(ExpBuilder))
}

object DataValueAstBuilder: rmlBaseVisitor<DataValue>() {
    override fun visitSimpleVal(ctx: rmlParser.SimpleValContext?): SimpleValue =
            ctx!!.accept(SimpleValueAstBuilder)
    override fun visitObjectVal(ctx: rmlParser.ObjectValContext?) =
            ObjectValue(ctx!!.`object`().field().map(::buildFieldAst).toList())
    override fun visitListVal(ctx: rmlParser.ListValContext?) = ListValue(
            ctx!!.value()?.map { it.accept(this) } ?: emptyList(),
            ctx.ellipsis() != null)
    override fun visitOrPatternVal(ctx: rmlParser.OrPatternValContext?) =
            OrPatternValue(ctx!!.value(0).accept(this), ctx.value(1).accept(this))
}

object SimpleValueAstBuilder: rmlBaseVisitor<SimpleValue>() {
    override fun visitUnusedVal(ctx: rmlParser.UnusedValContext?) = UnusedValue
    override fun visitVarValue(ctx: rmlParser.VarValueContext?) = VarValue(ctx!!.LOWERCASE_ID().text)
    override fun visitIntValue(ctx: rmlParser.IntValueContext?) = IntValue(ctx!!.INT().text.toInt())
    override fun visitBooleanValue(ctx: rmlParser.BooleanValueContext?) =
            BooleanValue(ctx!!.BOOLEAN().text!!.toBoolean())
    override fun visitStringValue(ctx: rmlParser.StringValueContext?) =
            StringValue(ctx!!.text.removePrefix("'").removeSuffix("'"))
    override fun visitListValue(ctx: rmlParser.ListValueContext?) = ListSimpleValue(
            ctx!!.simpleValues()?.simpleValue()?.map { it.accept(this) } ?: emptyList(),
            ctx.ellipsis() != null)
}

object TraceExpAstBuilder: rmlBaseVisitor<TraceExp>() {
    override fun visitCatTExp(ctx: rmlParser.CatTExpContext?): ConcatTraceExp =
            visitBinTExp(ctx!!.texp(0), ctx.texp(1), ::ConcatTraceExp)

    override fun visitAndTExp(ctx: rmlParser.AndTExpContext?): AndTraceExp =
            visitBinTExp(ctx!!.texp(0), ctx.texp(1), ::AndTraceExp)

    override fun visitOrTExp(ctx: rmlParser.OrTExpContext?): OrTraceExp =
            visitBinTExp(ctx!!.texp(0), ctx.texp(1), ::OrTraceExp)

    override fun visitShufTExp(ctx: rmlParser.ShufTExpContext?): ShuffleTraceExp =
            visitBinTExp(ctx!!.texp(0), ctx.texp(1), ::ShuffleTraceExp)

    override fun visitFilterExp(ctx: rmlParser.FilterExpContext?) = FilterTraceExp(
            buildEventTypeAst(ctx!!.evtype()),
            ctx.texp(0).accept(this),
            ctx.texp(1)?.accept(this) ?: AllTraceExp
    )

    override fun visitCondFilterExp(ctx: rmlParser.CondFilterExpContext?) = CondFilterTraceExp(
            buildEventTypeAst(ctx!!.evtype()),
            ctx.texp(0).accept(this),
            ctx.texp(1)?.accept(this) ?: AllTraceExp
    )

    override fun visitStarTExp(ctx: rmlParser.StarTExpContext?) =
            StarTraceExp(ctx!!.texp().accept(this))

    override fun visitPlusTExp(ctx: rmlParser.PlusTExpContext?) =
            PlusTraceExp(ctx!!.texp().accept(this))

    override fun visitOptionalTExp(ctx: rmlParser.OptionalTExpContext?) =
            OptionalTraceExp(ctx!!.texp().accept(this))

    override fun visitClosureTExp(ctx: rmlParser.ClosureTExpContext?) =
            ClosureTraceExp(ctx!!.texp().accept(this))

    override fun visitEmptyTExp(ctx: rmlParser.EmptyTExpContext?) = EmptyTraceExp

    override fun visitNoneTExp(ctx: rmlParser.NoneTExpContext?) = NoneTraceExp

    override fun visitAllTExp(ctx: rmlParser.AllTExpContext?) = AllTraceExp

    override fun visitBlockTExp(ctx: rmlParser.BlockTExpContext?): BlockTraceExp =
            BlockTraceExp(
                    visitVarsAux(ctx!!.vars()),
                    ctx.texp().accept(this)
            )

    override fun visitVarTExp(ctx: rmlParser.VarTExpContext?): TraceExpVar =
            TraceExpVar(
                    TraceExpId(ctx!!.UPPERCASE_ID().text),
                    ctx.exp().map { it.accept(ExpBuilder) }
            )

    override fun visitEvtypeTExp(ctx: rmlParser.EvtypeTExpContext?) =
            EventTypeTraceExp(buildEventTypeAst(ctx!!.evtype()))

    override fun visitEvtypeWithTExp(ctx: rmlParser.EvtypeWithTExpContext?) =
            EventTypeWithTraceExp(buildEventTypeAst(ctx!!.evtype()), ctx.exp().accept(ExpBuilder))

    override fun visitParTExp(ctx: rmlParser.ParTExpContext?): TraceExp =
            ctx!!.texp().accept(this)

    override fun visitIfElseTExp(ctx: rmlParser.IfElseTExpContext?) = IfElseTraceExp(
            ctx!!.exp().accept(ExpBuilder),
            ctx.texp(0).accept(this),
            ctx.texp(1).accept(this)
    )

    private fun <T: BinaryTraceExp> visitBinTExp(
            left: rmlParser.TexpContext,
            right: rmlParser.TexpContext,
            constructor: (TraceExp, TraceExp) -> T): T =
            constructor(left.accept(this), right.accept(this))
}

object ExpBuilder: rmlBaseVisitor<Exp>() {
    override fun visitBoolExp(ctx: rmlParser.BoolExpContext?) =
            BoolExp(ctx!!.BOOLEAN().text!!.toBoolean())

    override fun visitIntExp(ctx: rmlParser.IntExpContext?) =
            IntExp(ctx!!.INT().text.toInt())

    override fun visitFloatExp(ctx: rmlParser.FloatExpContext?) =
            FloatExp(ctx!!.FLOAT().text.toDouble())

    override fun visitVarExp(ctx: rmlParser.VarExpContext?) =
            VarExp(VarId(ctx!!.text))

    override fun visitSumExp(ctx: rmlParser.SumExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::SumExp)

    override fun visitSubExp(ctx: rmlParser.SubExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::SubExp)

    override fun visitMulExp(ctx: rmlParser.MulExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::MulExp)

    override fun visitDivExp(ctx: rmlParser.DivExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::DivExp)

    override fun visitLessThanExp(ctx: rmlParser.LessThanExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::LessThanExp)

    override fun visitLessThanEqExp(ctx: rmlParser.LessThanEqExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::LessThanEqExp)

    override fun visitGreaterThanExp(ctx: rmlParser.GreaterThanExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::GreaterThanExp)

    override fun visitGreaterThanEqExp(ctx: rmlParser.GreaterThanEqExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::GreaterThanEqExp)

    override fun visitEqualToExp(ctx: rmlParser.EqualToExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::EqualToExp)

    override fun visitAssignExp(ctx: rmlParser.AssignExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::AssignExp)

    override fun visitAndExp(ctx: rmlParser.AndExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::AndExp)

    override fun visitOrExp(ctx: rmlParser.OrExpContext?) =
            visitBinExp(ctx!!.exp(0), ctx.exp(1), ::OrExp)

    override fun visitUnaryMinusExp(ctx: rmlParser.UnaryMinusExpContext?) =
            UnaryMinusExp(ctx!!.exp().accept(this))

    override fun visitParenExp(ctx: rmlParser.ParenExpContext?): Exp =
            ctx!!.exp().accept(this)

    private fun visitBinExp(leftCtx: rmlParser.ExpContext,
                            rightCtx: rmlParser.ExpContext,
                            constructor: (Exp, Exp) -> Exp): Exp =
            constructor(leftCtx.accept(this), rightCtx.accept(this))
}

// visitVars already exists in BaseVisitor with the same signature, avoid confusion
private fun visitVarsAux(ctx: rmlParser.VarsContext?): List<VarId> =
        ctx?.LOWERCASE_ID()?.map { it.text }?.map(::VarId) ?: emptyList()