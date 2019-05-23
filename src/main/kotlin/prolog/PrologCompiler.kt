package prolog

import prolog.ast.*
import java.io.BufferedWriter

class PrologCompiler(private val writer: BufferedWriter) {
    private fun compile(term: PrologTerm): Unit = when (term) {
        is IntTerm -> writer.write(term.number.toString())
        is FloatTerm -> writer.write(term.number.toString())
        is VarTerm -> writer.write(term.name)
        is FunctionTerm -> compile(term)
        is ConstantTerm -> writer.write("'${term.string}'")
        is StringTerm -> writer.write("\"${term.string}\"")
        is PredicateIndicatorTerm -> writer.write("${term.name}/${term.arity}")
        is DictionaryTerm -> {
            writer.write("_{")

            if (term.map.entries.isNotEmpty()) {
                val first = term.map.entries.first()
                compile(first.key)
                writer.write(":")
                compile(first.value)
            }

            term.map.entries.drop(1).map {
                writer.write(",")
                compile(it.key)
                writer.write(":")
                compile(it.value)
            }

            writer.write("}")
        }
        is ListTerm -> {
            writer.write("[")

            // note that ListTerm ensures tail term is only allowed in presence of head terms before them

            // head terms
            if (term.headTerms.isNotEmpty()) {
                compile(term.headTerms[0])
                for (headTerm in term.headTerms.drop(1)) {
                    writer.write(", ")
                    compile(headTerm)
                }
            }

            // tail
            if (term.tail != null) {
                writer.write("|")
                compile(term.tail)
            }

            writer.write("]")
        }
    }

    private fun compile(term: FunctionTerm) {
        val functor = term.functionSymbol.name

        // constants
        if (term.arity == 0)
            writer.write(functor)
        // use infix notation for binary symbolic functors (unless it's something like ',' or ';')
        else if (term.arity == 2 && functor.matches(Regex("[^a-zA-Z0-9_']*"))) {
            // avoid ambiguity using parentheses
            writer.write("(")
            compile(term.args[0])
            writer.write(functor)
            compile(term.args[1])
            writer.write(")")
        } else { // prefix notation otherwise
            writer.write(functor)
            writer.write("(")
            compile(term.args.first())
            for (arg in term.args.drop(1)) {
                writer.write(", ")
                compile(arg)
            }
            writer.write(")")
        }
    }

    private fun compile(atom: Atom) {
        if (atom.negated)
            writer.write("not(")

        val symbol = atom.symbol.name

        // print unification as infix
        if (symbol == "=") {
            assert(atom.args.size == 2) { "unification must have 2 arguments" }
            compile(atom.args.first())
            writer.write(symbol)
            compile(atom.args[1])
        } else { // prefix syntax otherwise
            writer.write(symbol)

            if (atom.arity > 0) {
                writer.write("(")
                compile(atom.args.first())
                for (arg in atom.args.drop(1)) {
                    writer.write(", ")
                    compile(arg)
                }
                writer.write(")")
            }
        }

        if (atom.negated)
                writer.write(")")
    }

    private fun compile(clause: Clause) {
        compile(clause.head)

        if (clause.body.isNotEmpty()) {
            writer.write(" :- ")
            compile(clause.body.first())
            for (atom in clause.body.drop(1)) {
                writer.write(", ")
                compile(atom)
            }
        }

        writer.write(".\n")
    }

    private fun compile(directive: Directive) {
        writer.write(":- ")
        compile(directive.body.first())
        for (atom in directive.body.drop(1)) {
            writer.write(", ")
            compile(atom)
        }
        writer.write(".\n")
    }

    fun compile(program: LogicProgram) {
        program.directives.map(::compile)
        program.clauses.map(::compile)
    }
}