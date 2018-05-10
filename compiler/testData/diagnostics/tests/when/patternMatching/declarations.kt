data class A(val f: Int, val s: Int)

data class Pair<F, S>(val first: F, val second: S)

class B(val f: Int, val s: Int)

data class C<F, S>(val f: F, val s: S)

fun foo(x: Any?, a: Int) = when (<!DEBUG_INFO_SMARTCAST!>x<!>) {
    is like val <!NAME_SHADOWING, UNUSED_VARIABLE!>x<!> -> { val <!UNUSED_VARIABLE!>n<!> = 10; 10 }
    is like (<!COMPONENT_FUNCTION_MISSING!>val <!NAME_SHADOWING!>a<!> is Int<!>, <!COMPONENT_FUNCTION_MISSING!>val <!UNUSED_VARIABLE!>b<!> is Int<!>) -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
    is like Pair(val c is Int, val d is Int) -> <!DEBUG_INFO_SMARTCAST!>c<!> + <!DEBUG_INFO_SMARTCAST!>d<!>
    is like Pair(val <!UNUSED_VARIABLE!>c<!>, val <!UNUSED_VARIABLE!>d<!>) -> a + <!UNRESOLVED_REFERENCE!>b<!>
    is like A(val <!NAME_SHADOWING!>a<!>) -> a
    is like B(<!COMPONENT_FUNCTION_MISSING!>val <!NAME_SHADOWING!>a<!> is Int<!>) -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
    is like Pair?(<!COMPONENT_FUNCTION_ON_NULLABLE!>val <!NAME_SHADOWING!>a<!> is Int<!>, <!COMPONENT_FUNCTION_ON_NULLABLE!>val b is Int<!>) -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
    is like <!UNRESOLVED_REFERENCE!>pair<!>(<!COMPONENT_FUNCTION_MISSING!>val <!NAME_SHADOWING!>a<!> is Int<!>, <!COMPONENT_FUNCTION_MISSING!>val b is Int<!>) -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
    is like <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!NAME_SHADOWING!>a<!> is Int, val b is Int) && <!DEBUG_INFO_SMARTCAST!>a<!> > <!DEBUG_INFO_SMARTCAST!>b<!> -> <!DEBUG_INFO_SMARTCAST!>a<!> + <!DEBUG_INFO_SMARTCAST!>b<!>
    is like <!USELESS_TYPE_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(10, 20) -> a + <!UNRESOLVED_REFERENCE!>b<!>
    <!USELESS_IS_CHECK!>is <!NO_TYPE_ARGUMENTS_ON_RHS, DUPLICATE_LABEL_IN_WHEN!>Pair<!><!> -> 1
    is like <!USELESS_TYPE_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(_, val <!NAME_SHADOWING, UNUSED_VARIABLE!>a<!> is Pair<*, *>) -> 1
    is C<*, *> -> 2
    is like val <!NAME_SHADOWING!>a<!> is Int -> <!DEBUG_INFO_SMARTCAST!>a<!>
    else -> 1
}

fun foo2(x: Any) = when (x) {
    is Boolean, <!DEBUG_INFO_SMARTCAST!>x<!> is like Pair(<!NOT_ALLOW_PROPERTY_DEFINITION!>val a<!>) -> 1
    else -> 1
}
