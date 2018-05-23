
fun foo(a: Int) = when (a) {
    <!USELESS_IS_CHECK!>is like <!SYNTAX!><!>10<!> -> 10
    <!USELESS_IS_CHECK!>is like <!SYNTAX!><!>a<!> -> 10
    <!USELESS_IS_CHECK!>is like <!SYNTAX!><!>a + 3<!> -> 10
    <!USELESS_IS_CHECK!>is like <!SYNTAX!><!>field = <!SYNTAX!><!>10<!> -> 10
    is like val b -> b
    is like val b = 10-> b
}