package compiler.prolog.ast

import java.nio.charset.StandardCharsets

sealed class Term

data class IntTerm(val number: Int): Term()
data class FloatTerm(val number: Double): Term()

data class VariableTerm(val identifier: Identifier): Term() {
    // allow construction from strings
    constructor(identifier: String): this(Identifier(identifier))

    init {
        require(identifier.string.first().isUpperCase() || identifier.string.first() == '_') {
            "Prolog variables must start with uppercase letter or underscore"
        }
    }

    companion object {
        val ignored = VariableTerm("_")
    }
}

// f(term1, ..., termN) (including constants, strings...)
data class CompoundTerm(val functor: String, val args: List<Term>): Term() {
    val arity: Int = args.size

    // allow varargs
    constructor(functor: String, vararg args: Term): this(functor, args.toList())
}

// compound terms with arity 0 are often called atoms
fun atom(string: String) = CompoundTerm(string)

// tag{key1:value1, ..., keyN:valueN}
// they are first-class citizens in SWI-Prolog
data class DictionaryTerm(val tag: Term, val pairs: List<KeyValuePair>): Term() {
    data class KeyValuePair(val key: Term, val value: Term)
}

// handle lists in a special way
data class ListTerm(val list: List<Term> = emptyList(), val moreAllowed: Boolean = false): Term() {
    constructor(vararg terms: Term, moreAllowed: Boolean = false): this(terms.toList(), moreAllowed)

    init {
        if (list.isEmpty() && moreAllowed)
            error("unbound tail not allowed without head elements")
    }
}

// SWI-Prolog v7 strings
data class StringTerm(val string: String): Term()

data class Identifier(val string: String) {
    init {
        require(string.isNotBlank()) { "blank identifier not allowed" }
        require(StandardCharsets.US_ASCII.newEncoder().canEncode(string)) { "ASCII string expected" }
    }
}