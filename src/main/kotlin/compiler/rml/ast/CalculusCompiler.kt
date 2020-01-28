package compiler.rml.ast

import compiler.calculus.*
import compiler.calculus.Equation
import compiler.calculus.Identifier

// compile (part of) RML to trace calculus, which is parametric w.r.t. event types and data expressions

object CalculusCompiler {
    // intermediate results of compilation
    private var equations = mutableListOf<Equation<EventType, DataExpression>>()

    // immutable list of spec definitions, useful to avoid a second parameter in functions for static checking
    private var specDefs = listOf<compiler.rml.ast.Equation>()

    // returns true iff the expression denotes a set with the empty trace, computes the equivalent of E(t) in the
    // trace calculus
    // important remark: optimized version used *exclusively* to check that terms are contractive,
    // hence no infinite loop can ever occur and no check for non termination is needed
    fun hasEmptyTrace(expression: Expression): Boolean = when (expression) {
        is StarExpression -> true
        is PlusExpression -> hasEmptyTrace(expression.exp)
        is OptionalExpression -> true
        is PrefixClosureExpression -> true
        // more compact form is ConcatExpression, is AndExpression, is ShuffleExpression -> ... does not work
        // Unresolved reference: left/right
        is ConcatExpression -> hasEmptyTrace(expression.left) && hasEmptyTrace(expression.right)
        is AndExpression -> hasEmptyTrace(expression.left) && hasEmptyTrace(expression.right)
        is ShuffleExpression -> hasEmptyTrace(expression.left) && hasEmptyTrace(expression.right)
        is OrExpression -> hasEmptyTrace(expression.left) || hasEmptyTrace(expression.right)
        is FilterExpression ->
            hasEmptyTrace(expression.filteredExpression) && (expression.unfilteredExpression==null ||
                    hasEmptyTrace(expression.unfilteredExpression))
        is IfElseExpression -> // remark: this is an over-approximation, the check is static while E is dynamic
            hasEmptyTrace(expression.thenExpression) && hasEmptyTrace(expression.elseExpression)
        EmptyExpression -> true
        AllExpression -> true
        is BlockExpression -> hasEmptyTrace(expression.expression)
        is VariableExpression ->
            hasEmptyTrace(specDefs.find{expression.id.equals(it.identifier)}?.expression?:
            throw RuntimeException("Undefined identifier ${expression.id}"))
        is EventTypeExpression -> false
    }

    fun compile(specification: Specification): compiler.calculus.Specification<EventType, DataExpression> {
        assert(equations.isEmpty())
        specDefs = specification.equations
        equations = specification.equations.map { compile(it) }.toMutableList()
        val mainIdentifier = Identifier(specification.mainIdentifier.name)
        val result = Specification(equations, mainIdentifier)

        // avoid memory leaks by emptying the intermediate equations list
        equations = mutableListOf()

        return result
    }

    private fun compile(equation: compiler.rml.ast.Equation) =
            // non-generic equations compilation is straightforward
            if (equation.parameters.isEmpty())
                Equation(equation.identifier.name, compile(equation.expression))
            // S<x1, ..., xN> = E  --->  S = <x1, ..., xN>.E'     (with E ---> E')
            else
                Equation(equation.identifier.name, GenericExpression(
                        equation.parameters.map { Identifier(it.name) },
                        compile(equation.expression)
                ))

    // return expressions but update equations (lost of equations) along the way as needed
    private fun compile(expression: Expression):
            compiler.calculus.Expression<EventType, DataExpression> = when (expression) {
        is StarExpression -> StarExpression(compile(expression.exp))
        is PlusExpression -> PlusExpression(compile(expression.exp))
        is OptionalExpression -> OptionalExpression(compile(expression.exp))
        is PrefixClosureExpression -> PrefixClosureExpression(compile(expression.exp))
        is ConcatExpression -> ConcatenationExpression(compile(expression.left), compile(expression.right))
        is AndExpression -> AndExpression(compile(expression.left), compile(expression.right))
        is OrExpression -> OrExpression(compile(expression.left), compile(expression.right))
        is ShuffleExpression -> ShuffleExpression(compile(expression.left), compile(expression.right))
        is FilterExpression -> FilterExpression(
                expression.eventType,
                compile(expression.filteredExpression),
                compile(expression.unfilteredExpression ?: AllExpression)
        )
        is IfElseExpression -> ConditionalExpression(
                expression.condition,
                compile(expression.thenExpression),
                compile(expression.elseExpression)
        )
        EmptyExpression -> compiler.calculus.EmptyExpression
        AllExpression -> OneExpression
        is BlockExpression -> // for the block work one variable at a time
            if (expression.declaredVariables.isEmpty())
                compile(expression.expression)
            else {
                val firstId = Identifier(expression.declaredVariables.first().name)
                val remainingVariables = expression.declaredVariables.drop(1)
                // only create a block if there are actually variables left
                val tailExp =
                        if (remainingVariables.isNotEmpty()) BlockExpression(remainingVariables, expression.expression)
                        else expression.expression
                ParametricExpression(firstId, compile(tailExp))
            }
        is VariableExpression -> // this could have generic arguments instantiation
            if (expression.genericArguments.isEmpty())
                VariableExpression(expression.id.name)
            else
                GenericApplication(VariableExpression(expression.id.name), expression.genericArguments)
        is EventTypeExpression -> PrefixExpression(expression.eventType, compiler.calculus.EmptyExpression)
    }
}