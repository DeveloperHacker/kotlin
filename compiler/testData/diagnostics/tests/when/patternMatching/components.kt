
class A {
    operator fun component2() = 10
}

val a = A()
val b = a is like (<!COMPONENT_FUNCTION_MISSING!>_<!>, 10)