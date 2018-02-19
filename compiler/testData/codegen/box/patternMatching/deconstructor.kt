// WITH_RUNTIME

deconstructor inline fun <reified T> Any?.samePair(): Pair<T, T>? = when(this) {
    is like Pair(val a is T, val b is T) && a == b -> this as Pair<T, T>
    else -> null
}

deconstructor inline fun <reified T> Pair<T, T>.superSamePair() = samePair<T>()


fun box(): String {
    val a = Pair(1, 2)
    if (a is like samePair<Int>(1, 2)) {
        return "FAIL 1"
    }
    val b = Pair(1, 1)
    if (b is like samePair<String>(1, 1)) {
        return "FAIL 2"
    }
    if (b !is like samePair<Int>(1, 1)) {
        return "FAIL 3"
    }
    if (b !is like superSamePair(1, 1)) {
        return "FAIL 4"
    }
    val c = Pair("a", "a")
    if (c is like (val c, val d)) {
        if (c + d != "aa") return "FAIL 2"
    } else {
        return "FAIL 5"
    }
    val firstCase = c is like superSamePair("a", "a")
    val secondCase = c !is like superSamePair("a", "a")
    if (!firstCase || secondCase) {
        return "FAIL 6"
    }
    if (a is like superSamePair("a", "a")) {
        return "FAIL 7"
    }
    if (c !is like superSamePair("a", "a")) {
        return "FAIL 8"
    }
    return when (c) {
        is like samePair<Int>(val d, val e) && d.toString() == "a" -> "FAIL 9"
        is like samePair<String>(val d, val e) && d == "a" -> "OK"
        else -> "FAIL 10"
    }
}
