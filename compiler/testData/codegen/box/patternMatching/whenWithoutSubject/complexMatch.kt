// WITH_RUNTIME

import kotlin.test.assertEquals

data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int) {
    operator fun component1() = a + 1231

    operator fun component2() = b + 2318

    operator fun deconstruct() = A(a, b)
}

fun matcher(value: Any?, p1: Int, p2: Int, p3: Int, p4: Int): List<Int> = when {
    value is String -> listOf(0)
    value is like val m = B(val a, p2 + p3) -> listOf(1, a)
    value is like val m = A(val a, p2 + p3) -> listOf(2, a)
    value is like val m = Pair(5, 7) -> listOf(3)
    value is like val m = Pair(val a is Int, p1) -> listOf(4, a)
    value is like val m = List(is Int, is Int) ->listOf(5)
    value is like val m = Pair(val a is Int, val b is Int) && a > p1 -> listOf(6, a, b)
    value is like val m = Pair("some string $p4 with parameter", _) -> listOf(7)
    value is like val m = Pair(Int(), Pair(val a is Int, val b is Int)) -> listOf(8, a, b)
    value is like val m -> listOf(9)
}

fun box() : String {
    assertEquals(matcher("3D4R0V4", 0, 0, 0, 0), listOf(0))
    assertEquals(matcher(B(5, 6), 0, 4, 2, 0), listOf(1, 5))
    assertEquals(matcher(A(5, 6), 0, 3, 3, 0), listOf(2, 5))
    assertEquals(matcher(Pair(5, 7), 0, 0, 0, 0), listOf(3))
    assertEquals(matcher(Pair(1, 2), 2, 0, 0, 0), listOf(4, 1))
    assertEquals(matcher(listOf(1, 2, 3, 4, 5, 6, 7), 0, 0, 0, 0), listOf(5))
    assertEquals(matcher(Pair(1, 2), 0, 0, 0, 0), listOf(6, 1, 2))
    assertEquals(matcher(Pair("some string 4 with parameter", 7), 0, 0, 0, 4), listOf(7))
    assertEquals(matcher(Pair(1, Pair(3, 9)), 0, 0, 0, 0), listOf(8, 3, 9))
    assertEquals(matcher(10, 0, 0, 0, 0), listOf(9))
    return "OK"
}
