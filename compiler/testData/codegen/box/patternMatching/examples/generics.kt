// WITH_RUNTIME

import kotlin.test.assertEquals

data class MyData<T>(val value: T)

deconstructor inline fun <reified T> Any.MyData() = if (this is MyData<*> && value is T) this as MyData<T> else null

fun MyData<*>.type() = when (this) {
    is like MyData<Int>() -> "Int"
    is like MyData<Long>() -> "Long"
    is like MyData<Float>() -> "Float"
    is like MyData<Double>() -> "Double"
    is like MyData<String>() -> "String"
    else -> "*"
}

fun box(): String {
    assertEquals("Int", MyData(1).type())
    assertEquals("Long", MyData(1L).type())
    assertEquals("Float", MyData(1.0f).type())
    assertEquals("Double", MyData(1.0).type())
    assertEquals("String", MyData("1").type())
    assertEquals("*", MyData(true).type())
    assertEquals("*", MyData(MyData(1)).type())
    return "OK"
}
