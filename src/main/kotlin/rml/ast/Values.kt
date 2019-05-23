package rml.ast

sealed class DataValue {
    abstract fun unfoldOrPatterns(): Set<DataValue>
}

sealed class SimpleValue: DataValue() {
    override fun unfoldOrPatterns() = setOf(this)
}
data class StringValue(val string: String): SimpleValue()
data class IntValue(val number: Int): SimpleValue()
data class BooleanValue(val boolean: Boolean): SimpleValue()
data class VarValue(val id: VarId): SimpleValue() {
    constructor(id: String): this(VarId(id))
}
data class ListSimpleValue(
        val values: List<SimpleValue>,
        val hasEllipsis: Boolean = false): SimpleValue()
object UnusedValue: SimpleValue()

data class ObjectValue(val fields: List<Field>): DataValue() {
    data class Field(val key: KeyId, val value: DataValue) {
        constructor(key: String, value: DataValue): this(KeyId(key), value)

        fun unfoldOrPatterns(): Set<Field> =
                value.unfoldOrPatterns().map { Field(key, it) }.toSet()
    }

    override fun unfoldOrPatterns(): Set<DataValue> {
        if (fields.isEmpty())
            return setOf(this)

        // unfold every field
        val fieldUnfolding: List<Set<Field>> = fields.map { it.unfoldOrPatterns() }

        // consider all possible combinations of field
        val combinations: Set<Set<Field>> = cartesianProduct(fieldUnfolding)

        return combinations.map { ObjectValue(it.toList()) }.toSet()
    }

    // https://stackoverflow.com/a/714256/1202636

    private fun cartesianProduct(sets: List<Set<Field>>): Set<Set<Field>> =
            when (sets.size) {
                0 -> emptySet()
                1 -> sets.toSet()
                else -> cartesianProductAux(0, sets)
            }

    private fun cartesianProductAux(index: Int, sets: List<Set<Field>>): Set<MutableSet<Field>> {
        val ret = mutableSetOf<MutableSet<Field>>()
        if (index == sets.size) {
            ret.add(mutableSetOf())
        } else {
            for (field in sets.toList()[index]) {
                for (set in cartesianProductAux(index+1, sets)) {
                    set.add(field)
                    ret.add(set)
                }
            }
        }
        return ret
    }
}

data class ListValue(
        val values: List<DataValue>,
        val hasEllipsis: Boolean = false): DataValue() {
    constructor(head: DataValue, tail: ListValue, hasEllipsis: Boolean = false):
            this(listOf(head) + tail.values, hasEllipsis)

    init {
        require(values.isNotEmpty() || !hasEllipsis) {
            "ellipsis with no elements not supported yet"
        }
    }

    override fun unfoldOrPatterns(): Set<ListValue> {
        if (values.isEmpty())
            return setOf(this)

        val head = values.first()
        val tail = ListValue(values.drop(1))

        // combine every head with every tail
        val result: MutableSet<ListValue> = mutableSetOf()
        for (unfoldedHead in head.unfoldOrPatterns())
            for (unfoldedTail in tail.unfoldOrPatterns())
                result.add(ListValue(unfoldedHead, unfoldedTail, hasEllipsis))

        return result
    }
}

data class OrPatternValue(val left: DataValue, val right: DataValue): DataValue() {
    override fun unfoldOrPatterns() = left.unfoldOrPatterns().union(right.unfoldOrPatterns())
}