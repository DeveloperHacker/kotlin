
data class Base<T>(val a: Int, val b: T)

typealias A = Base<Int>

data class B(val a: Any, val b: Any)

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

fun foo14(a: A) = when (a) {
    is like (_, 2) -> 0
}

fun foo14(a: Any) = when (<!DEBUG_INFO_SMARTCAST!>a<!>) {
    is like B(1, 2) -> 1
    is B -> 4
    else -> 0
}

fun foo15(a: Any) = when (<!DEBUG_INFO_SMARTCAST!>a<!>) {
    is like B(_, val b is Int) -> <!DEBUG_INFO_SMARTCAST!>b<!>
    is B -> 4
    else -> 0
}

fun foo16(a: Any) = when (<!DEBUG_INFO_SMARTCAST!>a<!>) {
    is like B(val b) -> b
    is <!DUPLICATE_LABEL_IN_WHEN!>B<!> -> 4
    else -> 0
}

fun foo17(a: Any) = when (<!DEBUG_INFO_SMARTCAST!>a<!>) {
    is like B(_, val b) && b is Int -> b
    is B -> 4
    else -> 0
}
