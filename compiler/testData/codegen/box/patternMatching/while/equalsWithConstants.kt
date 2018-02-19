// WITH_RUNTIME

import kotlin.test.assertEquals

import java.util.Random

fun box() : String {
    val x: Int = Random().nextInt(5)
    var i = 0
    while (i !is like x) i++;
    assertEquals(i, x)
    return "OK"
}