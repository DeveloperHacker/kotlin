sealed class Parent

class Child1(val field1: Int): Parent()

class Child2(val field2: Int): Parent()


fun foo(parent: Parent) = when(parent) {
    is like val <!UNUSED_VARIABLE!>child<!> is Child1 -> <!DEBUG_INFO_SMARTCAST!>parent<!>.field1 + parent.<!UNRESOLVED_REFERENCE!>field2<!>
    !is like val _ is Child2 -> 10
    is like val <!UNUSED_VARIABLE!>child<!> -> <!DEBUG_INFO_SMARTCAST!>parent<!>.field2 + parent.<!UNRESOLVED_REFERENCE!>field1<!>
}

fun foo2(parent: Parent) = when(parent) {
    is like val child is Child1 -> <!DEBUG_INFO_SMARTCAST!>child<!>.field1 + child.<!UNRESOLVED_REFERENCE!>field2<!>
    !is like val _ is Child2 -> 10
    is like val child -> <!DEBUG_INFO_SMARTCAST!>child<!>.field2 + child.<!UNRESOLVED_REFERENCE!>field1<!>
}