data class Pair<F, S>(val first: F, val second: S)

fun foo1(x: Any?) = <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_SMARTCAST!>x<!>) {
    is like Pair(val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>) -> 0
    is <!NO_TYPE_ARGUMENTS_ON_RHS, DUPLICATE_LABEL_IN_WHEN!>Pair<!> -> 0
    is like <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> is Int, val <!UNUSED_VARIABLE!>b<!> is Int) -> 0
}

fun bar1(x: Any?) = when (<!DEBUG_INFO_SMARTCAST!>x<!>) {
    is like Pair(val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>) -> 0
    is <!NO_TYPE_ARGUMENTS_ON_RHS, DUPLICATE_LABEL_IN_WHEN!>Pair<!> -> 0
    is like <!DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> is Int, val <!UNUSED_VARIABLE!>b<!> is Int) -> 0
    else -> 0
}

fun foo2(x: Pair<Int, Int>) = when (x) {
    <!USELESS_IS_CHECK!>is Pair<!> -> 0
    is like <!USELESS_TYPE_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> is <!USELESS_TYPE_CHECK!>Int<!>, val <!UNUSED_VARIABLE!>b<!> is <!USELESS_TYPE_CHECK!>Int<!>) -> 0
    is like (val <!UNUSED_VARIABLE!>a<!> is <!USELESS_TYPE_CHECK!>Int<!>, val <!UNUSED_VARIABLE!>b<!> is <!USELESS_TYPE_CHECK!>Int<!>) -> 0
    else -> 0
}

fun bar2(x: Pair<Int, Int>) = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    <!USELESS_IS_CHECK!>is Pair<!> -> 0
    is like <!USELESS_TYPE_CHECK, DUPLICATE_LABEL_IN_WHEN!>Pair<!>(val <!UNUSED_VARIABLE!>a<!> is <!USELESS_TYPE_CHECK!>Int<!>, val <!UNUSED_VARIABLE!>b<!> is <!USELESS_TYPE_CHECK!>Int<!>) -> 0
    is like (val <!UNUSED_VARIABLE!>a<!> is <!USELESS_TYPE_CHECK!>Int<!>, val <!UNUSED_VARIABLE!>b<!> is <!USELESS_TYPE_CHECK!>Int<!>) -> 0
}

fun foo3(x: Pair<Int, Int>) = when (x) {
    is like (val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>) -> 0
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 0
}

fun bar3(x: Pair<Int, Int>) = when (x) {
    is like (val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>) -> 0
}

fun foo4(x: List<Int>) = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is like [val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>] -> 0
}

fun bar4(x: List<Int>) = when (x) {
    is like [val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>] -> 0
    else -> 0
}

fun foo5(x: List<Int>) = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is like <!USELESS_TYPE_CHECK!>List<!>[val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>] -> 0
}

fun bar5(x: List<Int>) = when (x) {
    is like <!USELESS_TYPE_CHECK!>List<!>[val <!UNUSED_VARIABLE!>a<!>, val <!UNUSED_VARIABLE!>b<!>] -> 0
    else -> 0
}
