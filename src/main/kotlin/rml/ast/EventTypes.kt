package rml.ast

sealed class EvtypeDecl(open val evtype: EventType, open val negated: Boolean = false, open val guard: Exp? = null)
data class DirectEvtypeDecl(override val evtype: EventType,
                            val patternValue: DataValue,
                            override val negated: Boolean = false,
                            override val guard: Exp? = null): EvtypeDecl(evtype, negated) {
    init {
        require(checkValidTopObjects(patternValue)) {
            "(or-pattern of) objects expected at top-level event type declaration"
        }
    }

    private fun checkValidTopObjects(value: DataValue): Boolean = when (value) {
        is ObjectValue, UnusedValue -> true
        is OrPatternValue -> checkValidTopObjects(value.left) && checkValidTopObjects(value.right)
        else -> false
    }
}
data class DerivedEvtypeDecl(override val evtype: EventType,
                             val parents: List<EventType>,
                             override val negated: Boolean = false,
                             override val guard: Exp? = null): EvtypeDecl(evtype, negated) {
    init {
        require(parents.isNotEmpty()) { "at least one parent expected" }
    }
}

data class EventType(val id: Id, val dataValues: List<SimpleValue>) {
    data class Id(val name: String): AbstractId(name)

    constructor(id: String, dataValues: List<SimpleValue>): this(Id(id), dataValues)
}