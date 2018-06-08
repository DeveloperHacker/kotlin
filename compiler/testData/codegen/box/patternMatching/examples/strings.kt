// WITH_RUNTIME

import kotlin.test.assertEquals

data class Wrapper<T>(val value: T)

deconstructor fun String.Int() = toIntOrNull()?.let { Wrapper(it) }
deconstructor fun String.Boolean() = when (this) {
    "true" -> Wrapper(true)
    "false" -> Wrapper(false)
    else -> null
}

fun parseOrNull(string: String): Int? {
    if (string.split(" ") is like [Int(val value1), Boolean(val multiply), val *other]) {
        return when (other) {
            is like [] && !multiply -> value1
            is like [Int(val value2)] && multiply -> value1 * value2
            else -> null
        }
    }
    return null
}

fun box(): String {
    assertEquals(1, parseOrNull("1 true 1"))
    assertEquals(1, parseOrNull("1 false"))
    assertEquals(6, parseOrNull("2 true 3"))
    assertEquals(178, parseOrNull("178 false"))
    assertEquals(null, parseOrNull("some text"))
    assertEquals(null, parseOrNull("1 true 1 some text"))
    assertEquals(null, parseOrNull("1 false some text"))
    assertEquals(null, parseOrNull("1 false some"))
    return "OK"
}
