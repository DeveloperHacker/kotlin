// WITH_RUNTIME

fun box(): String {
    val a = 1
    val x: Any = Pair(10, 2)
    while (x is like Pair(val a, a + 1)) return "OK"
    return "fail : x must be matched"
}