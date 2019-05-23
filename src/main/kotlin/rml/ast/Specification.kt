package rml.ast

// top-level trace expression specification
data class Specification(val evtypeDecls: List<EvtypeDecl>, val traceExpDecls: List<TraceExpDecl>) {
    init {
        require(traceExpDecls.any { it.id.name == "Main" }) { "\"Main\" expression not declared" }
        require(traceExpDecls.map { it.id }.distinct() == traceExpDecls.map { it.id }) { "trace expression declared multiple times" }
        require(evtypeDecls.map { Pair(it.evtype.id, it.evtype.dataValues.size) }.distinct().size == evtypeDecls.size) {
            "event type declared multiple times"
        }
    }
}

// possibly generic trace expression as an equation
data class TraceExpDecl(val id: TraceExpId, val vars: List<VarId>, val traceExp: TraceExp)