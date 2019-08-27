package compiler.prolog.ast

data class Program(val directives: List<Directive>, val clauses: List<Clause>)

// term0 :- term1, ..., termN. (possibly a fact with no body)
data class Clause(val head: Term, val body: List<Term>) {
    constructor(head: Term, vararg body: Term): this(head, body.toList())
}

// :- term.
data class Directive(val body: List<Term>) {
    init {
        require(body.isNotEmpty()) { "empty directive" }
    }

    constructor(vararg body: Term): this(body.toList())
}
