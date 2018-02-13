// WITH_RUNTIME

import kotlin.test.assertEquals

fun matcher(value: Any?) = when {
    value is like Pair(val a = Pair(val b is Int, _), Int()) -> listOf(0, a, b)
    value is like val x -> listOf(1, x)
}

fun box() : String {
    assertEquals(matcher(Pair(Pair(2, 3), 1)), listOf(0, Pair(2, 3), 2))
    assertEquals(matcher(Pair(Pair("1", "2"), 1)), listOf(1, Pair(Pair("1", "2"), 1)))
    return "OK"
}