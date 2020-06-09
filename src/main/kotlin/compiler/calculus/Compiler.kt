package compiler.calculus

import compiler.prolog.ast.*

// trace calculus to Prolog
// calculus AST is parametric w.r.t. event types and data expressions, need a way to compile them
class Compiler<ET, DE>(val eventTypeCompiler: (ET) -> Term, val dataExpressionCompiler: (DE) -> Term) {
    // specifications have names in the underlying Prolog implementation
    fun compile(specification: Specification<ET, DE>, name: String): Clause {
        val head = CompoundTerm("trace_expression",
                atom(name),
                VariableTerm(specification.mainIdentifier.name))
        val body = specification.equations.map { compile(it) }
        return Clause(head, body)
    }

    private fun compile(equation: Equation<ET, DE>) = CompoundTerm("=",
            VariableTerm(equation.identifier.name),
            compile(equation.expression))

    private fun compile(expression: Expression<ET, DE>): Term = when (expression) {
        EmptyExpression -> atom("eps")
        ZeroExpression -> IntTerm(0)
        OneExpression -> IntTerm(1)
        is EventTypePattern ->  // June 2020, Davide, support for singleton event type patterns
                eventTypeCompiler(expression.eventType)
        is PrefixExpression -> CompoundTerm(":",
                eventTypeCompiler(expression.eventType),
                compile(expression.expression))
        is ConcatenationExpression -> CompoundTerm("*",
                compile(expression.left),
                compile(expression.right))
        is AndExpression -> CompoundTerm("/\\",
                compile(expression.left),
                compile(expression.right))
        is OrExpression -> CompoundTerm("\\/",
                compile(expression.left),
                compile(expression.right))
        is ShuffleExpression -> CompoundTerm("|",
                compile(expression.left),
                compile(expression.right))
        is VariableExpression -> VariableTerm(expression.identifier.name)
        is ParametricExpression -> CompoundTerm("var",
                atom(expression.identifier.name),
                compile(expression.expression))
        is GenericExpression -> CompoundTerm("gen",
                ListTerm(expression.variables.map { atom(it.name) }),
                compile(expression.expression))
        is GenericApplication -> CompoundTerm("app",
                compile(expression.expression),
                ListTerm(expression.arguments.map(dataExpressionCompiler)))
        is ConditionalExpression -> CompoundTerm("guarded",
                dataExpressionCompiler(expression.condition),
                compile(expression.thenBranch),
                compile(expression.elseBranch))
        is StarExpression -> CompoundTerm("star", compile(expression.expression))
        is OptionalExpression -> CompoundTerm("optional", compile(expression.expression))
        is PlusExpression -> CompoundTerm("plus", compile(expression.expression))
        is PrefixClosureExpression -> CompoundTerm("clos", compile(expression.expression))
        // because of Prolog operator precedence, ET>>T1;T2 is (ET>>T1);T2
        is FilterExpression -> CompoundTerm(";",
                CompoundTerm(">>",
                        eventTypeCompiler(expression.eventType),
                        compile(expression.filteredExpression)),
                compile(expression.unfilteredExpression))
    }
}