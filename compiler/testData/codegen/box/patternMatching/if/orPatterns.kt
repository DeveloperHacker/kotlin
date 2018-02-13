// WITH_RUNTIME

import kotlin.test.assertEquals

fun matcher(any: Any?, y: Any?) =
    if (any is like eq y) "is like eq y"
    else if (any is String) "is String"
    else if (any !is like Pair(1, _) || any !is like Pair(_, 2)) "!is like Pair(1, _), !is like Pair(_, 2)"
    else if (any is like Pair(1, 2)) "is like Pair(1, 2)"
    else throw java.lang.UnsupportedOperationException("unexpected case")

fun box(): String {
    assertEquals(matcher("10", "10"), "is like eq y")
    assertEquals(matcher("10", "20"), "is String")
    assertEquals(matcher(1, 4), "!is like Pair(1, _), !is like Pair(_, 2)")
    assertEquals(matcher(1, 2), "!is like Pair(1, _), !is like Pair(_, 2)")
    assertEquals(matcher(Pair(1, 4), null), "!is like Pair(1, _), !is like Pair(_, 2)")
    assertEquals(matcher(Pair(1, 2), null), "is like Pair(1, 2)")
    return "OK"
}
