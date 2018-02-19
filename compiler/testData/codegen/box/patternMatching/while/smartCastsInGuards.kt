// WITH_RUNTIME

import kotlin.test.assertEquals
import kotlin.collections.listOf

sealed class Parent(val value: Int) {
    operator fun component1() = value
}

class Child1(val field1: Int): Parent(field1)

class Child2(val field2: Int): Parent(field2)

fun box(): String {
    var nodes = listOf(Child1(1), Child1(2), Child1(3), Child2(4), Child2(5), Child2(4), Child2(3), Child2(2), Child2(1), Child1(0))
    var prevNode: Parent = Child1(0)
    while (prevNode is like Child1() && nodes is like (val node = (val value)) && prevNode.field1 < value) {
        prevNode = node
        nodes = nodes.drop(1)
    }
    assertEquals(prevNode.component1(), 4)
    assertEquals(nodes.size, 6)
    prevNode = Child1(6)
    while (prevNode is like (val value) && nodes is like (val node is Child2) && value > node.field2) {
        prevNode = node
        nodes = nodes.drop(1)
    }
    assertEquals(prevNode.component1(), 1)
    assertEquals(nodes.size, 1)
    return "OK"
}

