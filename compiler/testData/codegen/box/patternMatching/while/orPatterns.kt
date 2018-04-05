// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    var a = Pair(1, 1)
    while (a !is like (10, _) || a !is like (_, 10)) {
        val (l, r) = a
        a = if (r > l) Pair(l + 1, r) else Pair(l, r + 1)
    }
    assertEquals(a, Pair(10, 10))
    return "OK"
}
