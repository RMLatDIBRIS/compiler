package compiler.rml.ast

// events coming from the instrumentation
sealed class EventExpression

object IgnoredEventExpression: EventExpression()
data class PatternEventExpression(val left: EventExpression, val right: EventExpression): EventExpression()
// moreAllowed is true if ellipsis at the end are used
data class ListEventExpression(val list: List<EventExpression>, val moreAllowed: Boolean): EventExpression()
data class StringEventExpression(val string: String): EventExpression()
data class IntEventExpression(val number: Int): EventExpression()
data class FloatEventExpression(val number: Double): EventExpression()
data class BoolEventExpression(val value: Boolean): EventExpression()
data class VariableEventExpression(val variable: Identifier): EventExpression() {
    // allow construction from String directly
    constructor(variable: String): this(Identifier(variable))
}

data class ObjectEventExpression(val fields: List<Field>): EventExpression() {
    data class Field(val key: Identifier, val value: EventExpression) {
        // allow construction from String directly
        constructor(key: String, value: EventExpression): this(Identifier(key), value)
    }
}