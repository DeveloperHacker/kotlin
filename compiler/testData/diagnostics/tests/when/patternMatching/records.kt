
data class A(val first: Int, val second: Int, val third: Int)

class B(val first: Int, val second: Int, val third: Int)

fun foo(a: A) = when(a) {
    is like (first = 10, second = 20) -> 1
    is like (second = 20, first = 10) -> 1
    is like (first = 10) -> 1
    is like (10, first = 10) -> 1
    is like (10, first = 10,<!SYNTAX!><!> 20) -> 1
    is like (10, second = 10) -> 1
    is like (10, second = 10,<!SYNTAX!><!> 20) -> 1
    is like (10, third = 10) -> 1
    is like (10, third = 10, second = 20) -> 1
    else -> 1
}

fun foo(a: B) = when(a) {
    is like (first = 10, second = 20) -> 1
    is like (second = 20, first = 10) -> 1
    is like (first = 10) -> 1
    is like (<!COMPONENT_FUNCTION_MISSING!>10<!>, first = 10) -> 1
    is like (<!COMPONENT_FUNCTION_MISSING!>10<!>, first = 10,<!SYNTAX!><!> <!COMPONENT_FUNCTION_MISSING!>20<!>) -> 1
    is like (<!COMPONENT_FUNCTION_MISSING!>10<!>, second = 10) -> 1
    is like (<!COMPONENT_FUNCTION_MISSING!>10<!>, second = 10,<!SYNTAX!><!> <!COMPONENT_FUNCTION_MISSING!>20<!>) -> 1
    is like (<!COMPONENT_FUNCTION_MISSING!>10<!>, third = 10) -> 1
    is like (<!COMPONENT_FUNCTION_MISSING!>10<!>, third = 10, second = 20) -> 1
    else -> 1
}
