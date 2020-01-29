package compiler.rml.ast

sealed class Expression

// regex-like unary operators
data class StarExpression(val exp: Expression): Expression()
data class PlusExpression(val exp: Expression): Expression()
data class OptionalExpression(val exp: Expression): Expression()

data class PrefixClosureExpression(val exp: Expression): Expression()

// binary operators
data class ConcatExpression(val left: Expression, val right: Expression): Expression()
data class AndExpression(val left: Expression, val right: Expression): Expression()
data class OrExpression(val left: Expression, val right: Expression): Expression()
data class ShuffleExpression(val left: Expression, val right: Expression): Expression()

// conditional operators
data class FilterExpression(val eventType: EventType,
                            val filteredExpression: Expression,
                            val unfilteredExpression: Expression?): Expression()
data class IfElseExpression(val condition: DataExpression,
                            val thenExpression: Expression,
                            val elseExpression: Expression): Expression()

// constants
object EmptyExpression: Expression()
object AllExpression: Expression()

// block with variables declaration
data class BlockExpression(val declaredVariables: List<Identifier>, val expression: Expression): Expression() {
    init {
        require(declaredVariables.isNotEmpty()) { "at least one variable declaration expected in block" }
    }
}

// trace expression identifier with possibly generic arguments
data class VariableExpression(val id: Identifier, val genericArguments: List<DataExpression> = listOf()): Expression() {
    // allow construction from String directly
    constructor(id: String, genericArguments: List<DataExpression>): this(Identifier(id), genericArguments)
}

data class EventTypeExpression(val eventType: EventType): Expression()