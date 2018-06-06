// WITH_RUNTIME

import kotlin.test.assertEquals

sealed class JoinList<T>

class Empty<T>: JoinList<T>()

data class Single<T>(val value: T): JoinList<T>()

data class Atomic<T>(val list: List<T>): JoinList<T>()

data class Cons<T>(val left: JoinList<T>, val right: JoinList<T>): JoinList<T>()

infix fun <T> JoinList<T>.join(other: JoinList<T>) = Cons(this, other)

infix fun <T> JoinList<T>.join(other: List<T>) = Cons(this, Atomic(other))

infix fun <T> List<T>.join(other: JoinList<T>) = Cons(Atomic(this), other)

infix fun <T> List<T>.join(other: List<T>) = Cons(Atomic(this), Atomic(other))

val JoinList<*>.size: Int
    get() = when (this) {
        is Empty -> 0
        is Single -> 1
        is like Atomic(val list) -> list.size
        is like Cons(val left, val right) -> left.size + right.size
    }

fun box(): String {
    assertEquals(4, (listOf(1, 2) join listOf(3, 4)).size)
    assertEquals(6, (listOf(1, 2) join listOf(3, 4) join listOf(5, 6)).size)
    assertEquals(8, (listOf(1, 2) join listOf(3, 4) join listOf(5, 6) join listOf(7, 8)).size)
    assertEquals(8, ((listOf(1, 2) join listOf(3, 4)) join listOf(5, 6) join listOf(7, 8)).size)
    assertEquals(8, (listOf(1, 2) join listOf(3, 4) join (listOf(5, 6) join listOf(7, 8))).size)
    assertEquals(8, ((listOf(1, 2) join listOf(3, 4)) join (listOf(5, 6) join listOf(7, 8))).size)
    assertEquals(8, (((listOf(1, 2) join listOf(3, 4)) join listOf(5, 6)) join listOf(7, 8)).size)
    return "OK"
}
