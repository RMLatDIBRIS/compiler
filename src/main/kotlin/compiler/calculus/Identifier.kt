package compiler.calculus

import java.nio.charset.StandardCharsets

data class Identifier(val name: String) {
    init {
        require(name.isNotBlank()) { "blank identifier not allowed" }
        require(StandardCharsets.US_ASCII.newEncoder().canEncode(name)) { "ASCII string expected" }
    }
}