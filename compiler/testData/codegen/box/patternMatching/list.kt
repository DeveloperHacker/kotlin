// WITH_RUNTIME

import kotlin.test.assertEquals

fun test1(): Boolean {
    val list = listOf(1, 2, 3, 4)
    val trueCase = list is like [1, 2, 3, 4]
    val falseCase = list !is like [1, 2, 3, 4]
    return trueCase && !falseCase
}

fun test2(): Boolean {
    val list = listOf(1, 2, 3, 4)
    val trueCase = list is like [_, _, _, _]
    val falseCase = list !is like [_, _, _, _]
    return trueCase && !falseCase
}

fun test3(): Boolean {
    val list = listOf(1, 2, 3, 4)
    val trueCase = list !is like [1, 2]
    val falseCase = list is like [1, 2]
    return trueCase && !falseCase
}

fun test4(): Boolean {
    val list = listOf(1, 2, 3, 4)
    val trueCase = list !is like [_, _]
    val falseCase = list is like [_, _]
    return trueCase && !falseCase
}

fun test5(): Boolean {
    val list = listOf(1, 2, 3, 4)
    val trueCase = list is like [_, *_]
    val falseCase = list !is like [_, *_]
    return trueCase && !falseCase
}

fun test6(): Boolean {
    val list = listOf(1, 2, 3, 4)
    val trueCase = list is like [1, *[2, *[3, 4]]]
    val falseCase = list !is like [1, *[2, *[3, 4]]]
    return trueCase && !falseCase
}

fun test7(): Boolean {
    val list = listOf(1, 2, 3, 4)
    val trueCase = list is like [_, *[_, *[_, _]]]
    val falseCase = list !is like [_, *[_, *[_, _]]]
    return trueCase && !falseCase
}

fun test8(): Boolean {
    val list = listOf(1, 2, 3, 4)
    if (list is like [val a, val *xs] && a == 1) {
        val trueCase = xs is like [2, 3, 4]
        return trueCase
    }
    return false
}

fun test9(): Boolean {
    val list = listOf(1, 2, 3, 4)
    if (list is like [val a, val *xs] && a == 1) {
        val falseCase = xs !is like [2, 3, 4]
        return !falseCase
    }
    return false
}

fun test10(): Boolean {
    val list = listOf(1, 2, 3, 4)
    if (list is like [val a, val *xs] && a == 1) {
        return xs is like[2, 3, 4] && xs is like []
    }
    return false
}

fun test11(): Boolean {
    val list = listOf(1, 2, 3, 4)
    val trueCase = list is like [_, _, _, _, *[]]
    val falseCase = list !is like [_, _, _, _, *[]]
    return trueCase && !falseCase
}

fun box(): String {
    assert(test1())
    assert(test2())
    assert(test3())
    assert(test4())
    assert(test5())
    assert(test6())
    assert(test7())
    assert(test8())
    assert(test9())
    assert(test10())
    assert(test11())
    return "OK"
}
