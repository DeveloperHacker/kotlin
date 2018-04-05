// WITH_RUNTIME

import kotlin.test.assertEquals

import java.util.Random

data class Ref<T>(val value: T)

deconstructor fun <T> T.Wrapper() = Ref<T>(this)

fun box() : String {
    val x: Int = Random().nextInt(5)
    var i = 0
    while (i !is like Wrapper(x)) i++;
    assertEquals(i, x)
    return "OK"
}