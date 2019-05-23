package rml.ast

sealed class TraceExp

sealed class BinaryTraceExp: TraceExp() {
    // declare it abstract and override it in subclasses to avoid duplicating the field
    // (can't define the property here because data classes need to define at least one property)
    abstract val left: TraceExp
    abstract val right: TraceExp
}

data class ConcatTraceExp(override val left: TraceExp, override val right: TraceExp): BinaryTraceExp()
data class AndTraceExp(override val left: TraceExp, override val right: TraceExp): BinaryTraceExp()
data class OrTraceExp(override val left: TraceExp, override val right: TraceExp): BinaryTraceExp()
data class ShuffleTraceExp(override val left: TraceExp, override val right: TraceExp): BinaryTraceExp()

data class ClosureTraceExp(val exp: TraceExp): TraceExp()

data class FilterTraceExp(val evtype: EventType,
                          val leftExp: TraceExp,
                          val rightExp: TraceExp = AllTraceExp): TraceExp()
data class CondFilterTraceExp(val evtype: EventType,
                              val leftExp: TraceExp,
                              val rightExp: TraceExp = AllTraceExp): TraceExp()
data class StarTraceExp(val exp: TraceExp): TraceExp()
data class PlusTraceExp(val exp: TraceExp): TraceExp()
data class OptionalTraceExp(val exp: TraceExp): TraceExp()
data class IfElseTraceExp(val condition: Exp, val thenTraceExp: TraceExp, val elseTraceExp: TraceExp): TraceExp()

object EmptyTraceExp: TraceExp()
object NoneTraceExp: TraceExp()
object AllTraceExp: TraceExp()

// scoped declaration of one or more variables
data class BlockTraceExp(val declaredVars: List<VarId>, val traceExp: TraceExp): TraceExp()

// occurrence of trace expression identifier with possibly generic arguments
data class TraceExpVar(val id: TraceExpId, val genericArgs: List<Exp>): TraceExp()

data class EventTypeTraceExp(val eventType: EventType): TraceExp()
data class EventTypeWithTraceExp(val eventType: EventType, val exp: Exp): TraceExp()