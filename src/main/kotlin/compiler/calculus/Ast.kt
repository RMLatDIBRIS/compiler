package compiler.calculus

// this calculus AST is parametric w.r.t. event types (ET) and data expressions (DE)
// AST nodes are read-only so covariance is fine, and it allows singletons (see below)

data class Specification<ET, DE>(val equations: List<Equation<ET, DE>>, val mainIdentifier: Identifier)

data class Equation<ET, DE>(val identifier: Identifier, val expression: Expression<ET, DE>) {
    // allow construction from String directly
    constructor(name: String, expression: Expression<ET, DE>): this(Identifier(name), expression)
}

sealed class Expression<out ET, out DE>

// these singletons can be used whatever the sets of event types and data expressions are
object EmptyExpression: Expression<Nothing, Nothing>()
object ZeroExpression: Expression<Nothing, Nothing>()
object OneExpression: Expression<Nothing, Nothing>()

data class PrefixExpression<ET, DE>(val eventType: ET, val expression: Expression<ET, DE>): Expression<ET, DE>()

// binary expressions
data class ConcatenationExpression<ET, DE>(val left: Expression<ET, DE>, val right: Expression<ET, DE>): Expression<ET, DE>()
data class AndExpression<ET, DE>(val left: Expression<ET, DE>, val right: Expression<ET, DE>): Expression<ET, DE>()
data class OrExpression<ET, DE>(val left: Expression<ET, DE>, val right: Expression<ET, DE>): Expression<ET, DE>()
data class ShuffleExpression<ET, DE>(val left: Expression<ET, DE>, val right: Expression<ET, DE>): Expression<ET, DE>()

data class VariableExpression(val identifier: Identifier): Expression<Nothing, Nothing>() {
    // allow construction from String directly
    constructor(name: String): this(Identifier(name))
}

// generics
data class ParametricExpression<ET, DE>(val identifier: Identifier, val expression: Expression<ET, DE>): Expression<ET, DE>()
data class GenericExpression<ET, DE>(val variables: List<Identifier>,
                                     val expression: Expression<ET, DE>) : Expression<ET, DE>()

// don't make the first argument a GenericExpression, that makes compilation too hard, let static analyzers do their job
data class GenericApplication<ET, DE>(val expression: Expression<ET, DE>,
                                      val arguments: List<DE>): Expression<ET, DE>()

data class ConditionalExpression<ET, DE>(val condition: DE,
                                         val thenBranch: Expression<ET, DE>,
                                         val elseBranch: Expression<ET, DE>): Expression<ET, DE>()

// derived regex operators
data class StarExpression<ET, DE>(val expression: Expression<ET, DE>): Expression<ET, DE>()
data class OptionalExpression<ET, DE>(val expression: Expression<ET, DE>): Expression<ET, DE>()
data class PlusExpression<ET, DE>(val expression: Expression<ET, DE>): Expression<ET, DE>()

data class PrefixClosureExpression<ET, DE>(val expression: Expression<ET, DE>): Expression<ET, DE>()

data class FilterExpression<ET, DE>(val eventType: ET,
                                    val filteredExpression: Expression<ET, DE>,
                                    val unfilteredExpression: Expression<ET, DE>): Expression<ET, DE>()