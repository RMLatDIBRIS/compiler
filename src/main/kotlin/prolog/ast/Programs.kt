package prolog.ast

data class Atom(val symbol: PredicateSymbol,
                val negated: Boolean = false,
                val args: List<PrologTerm>) {
    val arity = args.size

    data class PredicateSymbol(val name: String) {
        init {
            if (name.isBlank())
                throw IllegalArgumentException("empty predicate symbol")
        }
    }

    constructor(symbol: String, vararg args: PrologTerm):
            this(PredicateSymbol(symbol), false, args.toList())

    fun negate() = Atom(symbol, true, args)
}

data class Clause(val head: Atom, val body: List<Atom>) {
    constructor(head: Atom, vararg bodyAtoms: Atom): this(head, bodyAtoms.toList())
}

// :- atom.
data class Directive(val body: List<Atom>) {
    init {
        require(body.isNotEmpty()) { "empty directive" }
    }

    constructor(vararg body: Atom): this(body.toList())
}

data class LogicProgram(val directives: List<Directive>, val clauses: List<Clause>)