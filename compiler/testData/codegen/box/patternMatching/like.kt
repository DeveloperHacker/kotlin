// WITH_RUNTIME

import kotlin.test.assertEquals

interface Like

data class like(val v1: Int, val v2: Int): Like


fun test1() {
    val a = like(1, 2)
    assertEquals(a is like, true)
    assertEquals(a is Like, true)
    assertEquals(a is like (1, 2), true)
    assertEquals(a is like (1, 1), false)
    assertEquals(a is like like(1, 2), true)
    assertEquals(a is like like(1, 1), false)
}

fun test2() {
    val `like` = like(1, 2)
    assertEquals(`like` is like, true)
    assertEquals(`like` is Like, true)
    assertEquals(`like` is like (1, 2), true)
    assertEquals(`like` is like (1, 1), false)
    assertEquals(`like` is like like(1, 2), true)
    assertEquals(`like` is like like(1, 1), false)
}

fun test3() {
    val a = like(1, 2)
    var b = false
    b = a is like
    assertEquals(b, true)
    b = a is Like
    assertEquals(b, true)
    b = a is like (1, 2)
    assertEquals(b, true)
    b = a is like (1, 1)
    assertEquals(b, false)
    b = a is like like(1, 2)
    assertEquals(b, true)
    b = a is like like(1, 1)
    assertEquals(b, false)
}

fun test4() {
    val `like` = like(1, 2)
    var b = false
    b = `like` is like
    assertEquals(b, true)
    b = `like` is Like
    assertEquals(b, true)
    b = `like` is like (1, 2)
    assertEquals(b, true)
    b = `like` is like (1, 1)
    assertEquals(b, false)
    b = `like` is like like(1, 2)
    assertEquals(b, true)
    b = `like` is like like(1, 1)
    assertEquals(b, false)
}

fun box(): String {
    test1()
    test2()
    test3()
    test4()
    return "OK"
}
