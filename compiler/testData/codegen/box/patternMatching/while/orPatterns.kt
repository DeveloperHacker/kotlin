// WITH_RUNTIME

import kotlin.test.assertEquals

fun matcher(any: Any?, y: Any?) =
    if (any is like eq y) "is like eq y"
    else if (any is String) "is String"
    else if (any !is like Pair(1, _) || any !is like Pair(_, 2)) "!is like Pair(1, _), !is like Pair(_, 2)"
    else if (any is like Pair(1, 2)) "is like Pair(1, 2)"
    else throw java.lang.UnsupportedOperationException("unexpected case")

fun box(): String {
    var a = Pair(1, 1)
    while (a !is like (10, _) || a !is like (_, 10)) {
        val (l, r) = a
        a = if (r > l) Pair(l + 1, r) else Pair(l, r + 1)
    }
    assertEquals(a, Pair(10, 10))
    return "OK"
}
