// WITH_RUNTIME

data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int)

fun box(): String {
    val a = 10
    val x = Any();
    val b = when {
        x is like A(val a, val b is Int) -> a
        x is like Pair(val c, val d) -> a + a
        x is like A(val a) -> a
        x is like Pair(val a is Int, val b is Int) -> a + b
        else -> return "OK"
    }
    return "FAIL"
}