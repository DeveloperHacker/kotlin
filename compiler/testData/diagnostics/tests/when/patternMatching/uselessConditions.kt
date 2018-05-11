
data class Base<T>(val a: Int, val b: T)

typealias A = Base<Int>

fun foo1(a: A) = when (a) {
    <!USELESS_IS_CHECK!>is like val _<!> -> 0
}

fun foo2(a: A) = when (a) {
    <!USELESS_IS_CHECK!>is like _<!> -> 0
}

fun foo3(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    <!USELESS_IS_CHECK!>is like val _ is <!USELESS_TYPE_CHECK!>A<!><!> -> 0
}

fun foo4(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    <!USELESS_IS_CHECK!>is like _ is <!USELESS_TYPE_CHECK!>A<!><!> -> 0
}

fun foo5(a: A) = when (a) {
    <!USELESS_IS_CHECK!>is like <!USELESS_TUPLE_DECONSTRUCTION!>(val _, val _)<!><!> -> 0
}

fun foo6(a: A) = when (a) {
    <!USELESS_IS_CHECK!>is like <!USELESS_TUPLE_DECONSTRUCTION!>(_, _)<!><!> -> 0
}

fun foo7(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    <!USELESS_IS_CHECK!>is like <!USELESS_TYPE_CHECK!>A<!><!USELESS_TUPLE_DECONSTRUCTION!>(val _, val _)<!><!> -> 0
}

fun foo8(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    <!USELESS_IS_CHECK!>is like <!USELESS_TYPE_CHECK!>A<!><!USELESS_TUPLE_DECONSTRUCTION!>(_, _)<!><!> -> 0
}

fun foo9(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    is like (_ is <!USELESS_TYPE_CHECK!>Int<!>, _ is <!USELESS_TYPE_CHECK!>Int<!>) -> 0
}

fun foo10(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    is like (val _ is <!USELESS_TYPE_CHECK!>Int<!>, val _ is <!USELESS_TYPE_CHECK!>Int<!>) -> 0
}

fun foo11(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    is like <!USELESS_TYPE_CHECK!>A<!>(_ is <!USELESS_TYPE_CHECK!>Int<!>, _ is <!USELESS_TYPE_CHECK!>Int<!>) -> 0
}

fun foo12(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    is like <!USELESS_TYPE_CHECK!>A<!>(val _ is <!USELESS_TYPE_CHECK!>Int<!>, val _ is <!USELESS_TYPE_CHECK!>Int<!>) -> 0
}

fun foo13(a: Base<Base<Int>>) = when (a) {
    is like (_, <!USELESS_TUPLE_DECONSTRUCTION!>(_, _)<!>) -> 0
}
