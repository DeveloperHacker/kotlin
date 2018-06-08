// WITH_RUNTIME

import kotlin.test.assertEquals

sealed class Expression

enum class Operation { MUL, DIV, ADD, SUB }

data class Const<T>(val value: T): Expression()

data class Name(val name: String): Expression()

data class Binary(val left: Expression, val operation: Operation, val right: Expression): Expression()

fun simplify(expression: Expression): Expression = when (expression) {
    is like Binary(_, Operation.MUL, val right = Const(0)) -> right
    is like Binary(val left = Const(0), Operation.MUL, _) -> left
    is like Binary(val left, Operation.MUL, Const(1)) -> simplify(left)
    is like Binary(Const(1), Operation.MUL, val right) -> simplify(right)
    is like Binary(Const(val left is Int), Operation.MUL, Const(val right is Int)) -> Const(left * right)
    else -> expression
}

fun box(): String {
    assertEquals(Const(6), simplify(Binary(Const(2), Operation.MUL, Const(3))))
    assertEquals(Name("name"), simplify(Binary(Const(1), Operation.MUL, Name("name"))))
    assertEquals(Binary(Const(3), Operation.MUL, Name("name")), simplify(Binary(Const(1), Operation.MUL, Binary(Const(3), Operation.MUL, Name("name")))))
    assertEquals(Const(0), simplify(Binary(Name("name"), Operation.MUL, Const(0))))
    assertEquals(Name("name"), simplify(Name("name")))
    return "OK"
}
