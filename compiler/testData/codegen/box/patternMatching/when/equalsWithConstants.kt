// WITH_RUNTIME

import kotlin.test.assertEquals

import java.util.Random

data class Ref<T>(val value: T)

deconstructor fun <T> T.Wrapper() = Ref<T>(this)

fun box() : String {
    val x: Int = Random().nextInt(5)

    val str = when (x) {
        is like Wrapper(0) -> "zero"
        is like Wrapper(1) -> "one"
        is like Wrapper(2) -> "two"
        is like _ -> "many"
    }

    when (x) {
        0 -> assertEquals(str, "zero")
        1 -> assertEquals(str, "one")
        2 -> assertEquals(str, "two")
        else -> assertEquals(str, "many")
    }

    return "OK"
}