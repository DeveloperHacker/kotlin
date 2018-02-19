// WITH_RUNTIME

fun box(): String {
    val a = Pair(1, 2)
    val b = a is like Pair(1, 2)
    return "OK"
}
