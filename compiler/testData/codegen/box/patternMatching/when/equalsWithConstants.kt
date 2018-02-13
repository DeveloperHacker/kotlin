// WITH_RUNTIME

import kotlin.test.assertEquals

import java.util.Random

fun box() : String {
    val x: Int = Random().nextInt(5)

    val str = when (x) {
        is like 0 -> "zero"
        is like 1 -> "one"
        is like 2 -> "two"
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