package compiler.rml.ast

// boolean/numeric expressions used inside the specification (e.g. in generics)
sealed class DataExpression

// base cases
data class BoolDataExpression(val boolean: Boolean): DataExpression()
data class IntDataExpression(val int: Int): DataExpression()
data class FloatDataExpression(val double: Double): DataExpression()
data class VariableDataExpression(val variable: Identifier): DataExpression() {
    // allow construction from String directly
    constructor(variable: String): this(Identifier(variable))
}

// arithmetic functions and unary operators
data class MinusDataExpression(val exp: DataExpression): DataExpression()
data class AbsDataExpression(val exp: DataExpression): DataExpression()

// arithmetic binary expressions
data class SumDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class SubDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class MulDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class DivDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()

// relational binary expressions
data class LessThanDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class LessThanEqualDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class GreaterThanDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class GreaterThanEqualDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class EqualToDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class NotEqualToDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()

// boolean binary expressions
data class AndDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()
data class OrDataExpression(val left: DataExpression, val right: DataExpression): DataExpression()