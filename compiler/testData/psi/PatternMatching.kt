
data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int) {
    fun deconstruct() = A(a, b)
}

fun matcher(value: Any?, p1: Int, p2: Int, p3: Int, p4: Int): List<Int> = when (value) {
    is String -> listOf(0)
    is like val m = B(val a, p2 + p3) -> listOf(1, a)
    is like val m = A(val a, p2 + p3) -> listOf(2, a)
    is like val m = Pair(5, 7) -> listOf(3)
    is like val m = Pair(val a is Int, p1) -> listOf(4, a)
    is like val m = List(Int(), Int()) ->listOf(5)
    is like val m = Pair(val a is Int, val b is Int) && a > p1 -> listOf(6, a, b)
    is like val m = Pair("some string $p4 with parameter", _) -> listOf(7)
    is like val m = Pair(Int(), Pair(val a is Int, val b is Int)) -> listOf(8, a, b)
    is like val m -> listOf(9)
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun matcher(p: Any?) = when (p) {
    is String -> listOf(0)
    is like Pair(val a, Pair(val b, val c)) -> listOf(1, a, b, c)
    else -> listOf(2)
}

fun matcher(value: Any?) = when (value) {
    is like Pair(val a = Pair(val b is Int, _), Int()) -> listOf(0, a, b)
    is like val x -> listOf(1, x)
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun matcher(value: Any?, p1: Int, p2: Int, p3: Int, p4: Int): List<Int> = when {
    value is String -> listOf(0)
    value is like val m = B(val a, p2 + p3) -> listOf(1, a)
    value is like val m = A(val a, p2 + p3) -> listOf(2, a)
    value is like val m = Pair(5, 7) -> listOf(3)
    value is like val m = Pair(val a is Int, p1) -> listOf(4, a)
    value is like val m = List(Int(), Int()) ->listOf(5)
    value is like val m = Pair(val a is Int, val b is Int) && a > p1 -> listOf(6, a, b)
    value is like val m = Pair("some string $p4 with parameter", _) -> listOf(7)
    value is like val m = Pair(Int(), Pair(val a is Int, val b is Int)) -> listOf(8, a, b)
    value is like val m -> listOf(9)
}

fun matcher(value: Any?, p1: Int, p2: Int, p3: Int, p4: Int): List<Int> =
    if (value is String) listOf(0)
    else if (value is like val m = B(val a, p2 + p3)) listOf(1, a)
    else if (value is like val m = A(val a, p2 + p3)) listOf(2, a)
    else if (value is like val m = Pair(5, 7)) listOf(3)
    else if (value is like val m = Pair(val a is Int, p1)) listOf(4, a)
    else if (value is like val m = List(Int(), Int()))listOf(5)
    else if (value is like val m = Pair(val a is Int, val b is Int) && a > p1) listOf(6, a, b)
    else if (value is like val m = Pair("some string $p4 with parameter", _)) listOf(7)
    else if (value is like val m = Pair(Int(), Pair(val a is Int, val b is Int))) listOf(8, a, b)
    else if (value is like val m) listOf(9)
    else throw java.lang.IllegalStateException("Unexpected else")

fun foo(parent: Parent) =
    if (parent is like val child is Child1 && child.field1 == 1 && parent.field1 == 1) child.field1
    else if (parent is like val child is Child2 && child.field2 == 2 && parent.field2 == 2) child.field2
    else 10

fun foo(parent: Parent) = when {
    parent is like val child is Child1 && child.field1 == 1 && parent.field1 == 1 -> child.field1
    parent is like val child is Child2 && child.field2 == 2 && parent.field2 == 2 -> child.field2
    else -> 10
}

fun foo3(x: Pair<Int, Int>) = when (x) {
    is like (val a, val b) -> 0
    else -> 0
}

fun foo3(x: Pair<Int, Int>) =
    if (x is Int) 0
    else 0

fun foo3(x: Pair<Int, Int>) {
    val a = x is Int
    ("string")
}

fun box() : String {
    val a = Pair(1, 2)
    when (a) {
        is like (_, val d) -> {
            assertEquals(d, 2)
            return "OK"
        }
        else -> return "match fail"
    }
    return "fail when generation"
}

fun matcher(any: Any?, y: Any?) = when(any) {
    is like eq y -> "is like eq y"
    is String -> "is String"
    !is like Pair(1, _), !is like Pair(_, 2) -> "!is like Pair(1, _), !is like Pair(_, 2)"
    is like Pair(_, eq 2) -> throw java.lang.UnsupportedOperationException("unexpected case")
    is like Pair(1, 2) -> "is like Pair(1, 2)"
    else -> throw java.lang.UnsupportedOperationException("unexpected case")
}
