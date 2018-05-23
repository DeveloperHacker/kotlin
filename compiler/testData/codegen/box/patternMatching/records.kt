// WITH_RUNTIME

import kotlin.test.assertEquals

data class A(val first: Int, val second: Int, val third: Int)

class B(val first: Int, val second: Int, val third: Int)

class C<T>(val first: Int, val second: T)

fun foo(a: A) = when(a) {
    is like (first = 1, second = 2) -> 1
    is like (second = 3, first = 2) -> 2
    is like (first = 1) -> 3
    is like (third = 3) -> 4
    is like (2, first = 2) -> 5
    is like (_, 3, second = 3) -> 6
    is like (4, third = 7) -> 7
    is like (5, third = 6, second = 7) -> 8
    else -> 0
}

fun foo(b: B) = when(b) {
    is like (first = 1, second = 2) -> 1
    is like (second = 3, first = 2) -> 2
    is like (first = 1) -> 3
    is like (third = 3) -> 4
    else -> 0
}

fun foo(c: C<C<Int>>) = when (c) {
    is like (first = 1, second = (first = 2, second = 3)) -> 1
    is like (second = (first = 2, second = 3), first = 2) -> 2
    is like (second = (first = 2, second = 3)) -> 3
    is like (second = (first = 2)) -> 4
    is like (second = (second = 3)) -> 5
    is like (first = 1) -> 6
    else -> 0
}

fun box(): String {
    assertEquals(1, foo(B(1, 2, 0)))
    assertEquals(2, foo(B(2, 3, 0)))
    assertEquals(3, foo(B(1, 0, 0)))
    assertEquals(4, foo(B(0, 0, 3)))
    assertEquals(0, foo(B(0, 0, 0)))

    assertEquals(1, foo(A(1, 2, 0)))
    assertEquals(2, foo(A(2, 3, 0)))
    assertEquals(3, foo(A(1, 0, 0)))
    assertEquals(4, foo(A(0, 0, 3)))
    assertEquals(5, foo(A(2, 2, 0)))
    assertEquals(6, foo(A(0, 3, 0)))
    assertEquals(7, foo(A(4, 0, 7)))
    assertEquals(8, foo(A(5, 7, 6)))
    assertEquals(0, foo(A(0, 0, 0)))

    assertEquals(1, foo(C(1, C(2, 3))))
    assertEquals(2, foo(C(2, C(2, 3))))
    assertEquals(3, foo(C(0, C(2, 3))))
    assertEquals(4, foo(C(0, C(2, 0))))
    assertEquals(5, foo(C(0, C(0, 3))))
    assertEquals(6, foo(C(1, C(0, 0))))
    assertEquals(0, foo(C(0, C(0, 0))))
    return "OK"
}
