// WITH_RUNTIME

import kotlin.test.assertEquals

fun box() : String {
    val a = Pair(1, 2)
    while (a is like (_, val d)) {
        assertEquals(d, 2)
        return "OK"
    }
    return "FAIL"
}