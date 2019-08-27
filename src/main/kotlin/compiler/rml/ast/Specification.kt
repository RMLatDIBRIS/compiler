package compiler.rml.ast

// top-level RML specification
data class Specification(val eventTypeDeclarations: List<EventTypeDeclaration>,
                         val equations: List<Equation>,
                         val mainIdentifier: Identifier)

// possibly generic expression as an equation
data class Equation(val identifier: Identifier, val parameters: List<Identifier>, val expression: Expression) {
    // allow construction from String directly
    constructor(id: String, params: List<Identifier>, expression: Expression): this(Identifier(id), params, expression)
}