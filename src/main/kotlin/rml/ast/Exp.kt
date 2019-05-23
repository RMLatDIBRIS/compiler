package rml.ast

// boolean/integer expressions
sealed class Exp

// base cases
data class BoolExp(val boolean: Boolean): Exp()
data class IntExp(val int: Int): Exp()
data class FloatExp(val double: Double): Exp()
data class VarExp(val varId: VarId): Exp()

// unary expression
data class UnaryMinusExp(val exp: Exp): Exp()

// arithmetic binary expressions
data class SumExp(val left: Exp, val right: Exp): Exp()
data class SubExp(val left: Exp, val right: Exp): Exp()
data class MulExp(val left: Exp, val right: Exp): Exp()
data class DivExp(val left: Exp, val right: Exp): Exp()

// relational binary expressions
data class LessThanExp(val left: Exp, val right: Exp): Exp()
data class LessThanEqExp(val left: Exp, val right: Exp): Exp()
data class GreaterThanExp(val left: Exp, val right: Exp): Exp()
data class GreaterThanEqExp(val left: Exp, val right: Exp): Exp()
data class EqualToExp(val left: Exp, val right: Exp): Exp()
data class AssignExp(val left: Exp, val right: Exp): Exp()
data class AndExp(val left: Exp, val right: Exp): Exp()
data class OrExp(val left: Exp, val right: Exp): Exp()