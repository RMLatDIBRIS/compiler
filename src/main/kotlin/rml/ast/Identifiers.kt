package rml.ast

/* The only purpose of this class is to factorize common constructor checks.
   Cannot factorize the property itself since data subclasses need to define at least a property.
   Declaring the property abstract does not work since the init block would find it uninitialized.
   So, every data subclass must declare the property and forward it to the constructor.
 */
abstract class AbstractId(name: String) {
    // whatever the subclass, blank identifiers make no sense
    init {
        require(name.isNotBlank()) { "blank id not allowed" }
    }
}

// variables from parametric and generic trace expressions
data class VarId(val name: String): AbstractId(name)

// variables representing trace expression dataValues
data class TraceExpId(val name: String): AbstractId(name)

// object field key in event type definitions
data class KeyId(val name: String): AbstractId(name)