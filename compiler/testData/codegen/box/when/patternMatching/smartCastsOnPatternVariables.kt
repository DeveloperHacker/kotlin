// WITH_RUNTIME

sealed class Parent

class Child1(val field1: Int): Parent()

class Child2(val field2: Int): Parent()

fun foo1(parent: Parent) = when(parent) {
    is val child: Child1 -> parent.field1
    !is val _ : Child2 -> 10
    is val child -> parent.field2
    else -> 20
}

fun foo2(parent: Parent) = when(parent) {
    is val child: Child1 -> child.field1
    !is val _ : Child2 -> 10
    is val child -> child.field2
}

fun foo3(parent: Parent) = when(parent) {
    is val child: Child1 && child.field1 == 1 -> child.field1
    !is val _ : Child2 -> 10
    is val child && child.field2 == 2 -> child.field2
    else -> 20
}

fun box() = "OK"
