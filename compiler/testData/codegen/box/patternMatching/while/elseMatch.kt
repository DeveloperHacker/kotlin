// WITH_RUNTIME

data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int)

fun box(): String {
    val a = 10
    val x = Any();
    while (x is like A(val a, val b is Int)) return "FAIL"
    while (x is like Pair(val c, val d)) return "FAIL"
    while (x is like A(val a)) return "FAIL"
    while (x is like Pair(val a is Int, val b is Int)) return "FAIL"
    return "OK"

}