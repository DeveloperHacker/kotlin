// WITH_RUNTIME

import kotlin.test.assertEquals

import java.util.Random

fun box() : String {
    val x: Int = Random().nextInt(5)

    val str =
        if (x is like 0) "zero"
        else if (x is like 1) "one"
        else if (x is like 2) "two"
        else "many"

    when (x) {
        0 -> assertEquals(str, "zero")
        1 -> assertEquals(str, "one")
        2 -> assertEquals(str, "two")
        else -> assertEquals(str, "many")
    }

    return "OK"
}