// WITH_RUNTIME

fun box(): String {
    val x: Pair<Int, Int>? = null;
    while (x is like Pair(1, 2)) return "FAIL"
    return "OK"
}
