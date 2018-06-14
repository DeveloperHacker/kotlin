
fun foo(self: Any, a: Int, b: Boolean) = when (self) {
    is like 3 -> 10
    is like 3 <!SYNTAX!>> 3<!> -> 10
    is like 3 <!SYNTAX!>== 3<!> -> 10
    is like true <!SYNTAX!>&& a <= 3<!> -> 10
    is like true <!SYNTAX!>|| a <= 3<!> -> 10
    is like a -> 10
    is like a <!SYNTAX!>> 3<!> -> 10
    is like a <!SYNTAX!>== 3<!> -> 10
    is like a <!SYNTAX!>&& 3<!> -> 10
    is like a <!SYNTAX!>|| 3<!> -> 10
    is like a + 3 -> 10
    is like a + 3 <!SYNTAX!>> 3<!> -> 10
    is like a + 3 <!SYNTAX!>== 3<!> -> 10
    is like b <!SYNTAX!>&& a <= 3<!> -> 10
    is like b <!SYNTAX!>|| a <= 3<!> -> 10
    is like <!SYNTAX!><!>field = 3 -> 10
    is like <!SYNTAX!><!>field = a + 3 -> 10
    is like <!SYNTAX!><!>field = a + 3 <!SYNTAX!>> 3<!> -> 10
    is like <!SYNTAX!><!>field = a + 3 <!SYNTAX!>== 3<!> -> 10
    is like <!SYNTAX!><!>field = b <!SYNTAX!>&& a <= 3<!> -> 10
    is like <!SYNTAX!><!>field = b <!SYNTAX!>|| a <= 3<!> -> 10
    is like <!SYNTAX!><!>field = eq 3 -> 10
    is like <!SYNTAX!><!>field = eq a + 3 -> 10
    is like <!SYNTAX!><!>field = eq a + 3 <!SYNTAX!>> 3<!> -> 10
    is like <!SYNTAX!><!>field = eq a + 3 <!SYNTAX!>== 3<!> -> 10
    is like <!SYNTAX!><!>field = eq b <!SYNTAX!>&& a <= 3<!> -> 10
    is like <!SYNTAX!><!>field = eq b <!SYNTAX!>|| a <= 3<!> -> 10
    is like val field = eq 3 -> field
    is like val field = eq a + 3 -> field
    is like val field = eq a + 3 <!SYNTAX!>> 3<!> -> field
    is like val field = eq a + 3 <!SYNTAX!>== 3<!> -> field
    is like val field = eq a + 3 <!SYNTAX!>&& 3<!> -> field
    is like val field = eq a + 3 <!SYNTAX!>|| 3<!> -> field
    is like val field = 3 -> field
    is like val field = a + 3 -> field
    is like val field = a + 3 <!SYNTAX!>> 3<!> -> field
    is like val field = a + 3 <!SYNTAX!>== 3<!> -> field
    is like val field = b <!SYNTAX!>&& a <= 3<!> -> field
    is like val field = b <!SYNTAX!>|| a <= 3<!> -> field
    is like eq a + 3 -> 10
    is like eq a + 3 <!SYNTAX!>> 3<!> -> 10
    is like eq a + 3 <!SYNTAX!>== 3<!> -> 10
    is like eq b <!SYNTAX!>&& a <= 3<!> -> 10
    is like eq b <!SYNTAX!>|| a <= 3<!> -> 10
    is like eq (a + 3) -> 10
    is like eq (a + 3 > 3) -> 10
    is like eq (a + 3 == 3) -> 10
    is like eq (b && a <= 3) -> 10
    is like eq (b || a <= 3) -> 10
    else -> 10
}