// WITH_RUNTIME

import kotlin.test.assertEquals

class IntWrapper(val value: Int) {
    operator fun component1() = value

    operator fun plus(other: IntWrapper) = value + other.value

    operator fun plus(other: Int) = value + other

    override fun equals(other: Any?) =
        if (other is Equlitive) other == value
        else if (other is like IntWrapper(val o)) value == o
        else value == other
}

class Equlitive(val value: IntWrapper) {
    constructor(value: Int) : this(IntWrapper(value))

    operator fun component1() = value

    infix fun eq(other: Equlitive) = Equlitive(value + other.value)

    infix fun eq(other: IntWrapper) = value + other + 21

    infix fun eq(other: Int) = value + other + 21

    override fun equals(other: Any?) = 
        if (other is like Equlitive(val o)) o == value
        else if (other is like IntWrapper(val o)) o == value + 5
        else if (other is like val o is Int) o == value + 5
        else false
}

fun foo(a: Equlitive, eq: Int) = 
    if (a is like (eq)) 1
    else if (a is like (eq eq)) 2
    else if (a !is like (eq)) 4
    else if (a !is like (eq eq)) 5
    else 0

fun foo2(a: Equlitive, eq: Int) = 
    if (a is like (eq eq)) 2
    else if (a is like (eq)) 1
    else if (a !is like (eq eq)) 5
    else if (a !is like (eq)) 4
    else 0

fun foo3(a: Equlitive, eq: Int, _eq: Equlitive) = 
    if (a is like (eq)) 1
    else if (a is like (_eq)) 2
    else if (a is like (_eq eq eq)) 5
    else if (a is like (_eq eq _eq)) 6

    else if (a is like (eq eq)) 3
    else if (a is like (eq _eq)) 4
    else if (a is like (eq _eq eq eq)) 7
    else if (a is like (eq _eq eq _eq)) 8

    else if (a !is like (eq)) -1
    else if (a !is like (_eq)) -2
    else if (a !is like (_eq eq eq)) -5
    else if (a !is like (_eq eq _eq)) -6

    else if (a !is like (eq eq)) -3
    else if (a !is like (eq _eq)) -4
    else if (a !is like (eq _eq eq eq)) -7
    else if (a !is like (eq _eq eq _eq)) -8
    else 0

fun foo4(a: Equlitive, eq: Int, _eq: Equlitive) =
    if (a is like (eq eq)) 3
    else if (a is like (eq _eq)) 4
    else if (a is like (eq _eq eq eq)) 7
    else if (a is like (eq _eq eq _eq)) 8

    else if (a is like (eq)) 1
    else if (a is like (_eq)) 2
    else if (a is like (_eq eq eq)) 5
    else if (a is like (_eq eq _eq)) 6

    else if (a !is like (eq eq)) -3
    else if (a !is like (eq _eq)) -4
    else if (a !is like (eq _eq eq eq)) -7
    else if (a !is like (eq _eq eq _eq)) -8

    else if (a !is like (eq)) -1
    else if (a !is like (_eq)) -2
    else if (a !is like (_eq eq eq)) -5
    else if (a !is like (_eq eq _eq)) -6
    else 0

fun foo5(a: Equlitive, eq: Int, _eq: Equlitive) =
    if (a is like (eq eq)) 3
    else if (a is like (eq _eq)) 4
    else if (a is like (eq _eq eq eq)) 7
    else if (a is like (eq _eq eq _eq)) 8

    else if (a is like (eq)) 1
    else if (a is like (_eq)) 2
    else if (a is like (_eq eq eq)) 5
    else if (a is like (_eq eq _eq)) 6

    else if (a !is like (eq eq)) -3
    else if (a !is like (eq _eq)) -4
    else if (a !is like (eq _eq eq eq)) -7
    else if (a !is like (eq _eq eq _eq)) -8

    else if (a !is like (eq)) -1
    else if (a !is like (_eq)) -2
    else if (a !is like (_eq eq eq)) -5
    else if (a !is like (_eq eq _eq)) -6
    else 0

fun box(): String {
    assertEquals(1, foo(Equlitive(1), 1))
    assertEquals(4, foo(Equlitive(1), 3))

    assertEquals(2, foo2(Equlitive(1), 1))
    assertEquals(5, foo2(Equlitive(1), 3))

    assertEquals(1, foo3(Equlitive(1), 1, Equlitive(1)))
    assertEquals(2, foo3(Equlitive(6), 1, Equlitive(1)))
    assertEquals(5, foo3(Equlitive(23), 1, Equlitive(1)))
    assertEquals(6, foo3(Equlitive(7), 1, Equlitive(1)))
    assertEquals(-1, foo3(Equlitive(28), 1, Equlitive(1)))

    assertEquals(3, foo4(Equlitive(1), 1, Equlitive(1)))
    assertEquals(4, foo4(Equlitive(6), 1, Equlitive(1)))
    assertEquals(7, foo4(Equlitive(23), 1, Equlitive(1)))
    assertEquals(8, foo4(Equlitive(7), 1, Equlitive(1)))
    assertEquals(-3, foo4(Equlitive(28), 1, Equlitive(1)))

    assertEquals(4, foo5(Equlitive(6), 1, Equlitive(1)))
    assertEquals(7, foo5(Equlitive(23), 1, Equlitive(1)))

    return "OK"
}
