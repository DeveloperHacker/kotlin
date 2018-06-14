
fun foo(a: Int) = when (a) {
    is like 3 -> 10
    is like 3 > 3 -> 10
    is like 3 == 3 -> 10
    is like 3 && 3 -> 10
    is like 3 || 3 -> 10
    is like a -> 10
    is like a > 3 -> 10
    is like a == 3 -> 10
    is like a && 3 -> 10
    is like a || 3 -> 10
    is like a + 3 -> 10
    is like a + 3 > 3 -> 10
    is like a + 3 == 3 -> 10
    is like a + 3 && 3 -> 10
    is like a + 3 || 3 -> 10
    is like <!SYNTAX!><!>field = 3 -> 10
    is like <!SYNTAX!><!>field = a + 3 -> 10
    is like <!SYNTAX!><!>field = a + 3 > 3 -> 10
    is like <!SYNTAX!><!>field = a + 3 == 3 -> 10
    is like <!SYNTAX!><!>field = a + 3 && 3 -> 10
    is like <!SYNTAX!><!>field = a + 3 || 3 -> 10
    is like <!SYNTAX!><!>field = eq 3 -> 10
    is like <!SYNTAX!><!>field = eq a + 3 -> 10
    is like <!SYNTAX!><!>field = eq a + 3 > 3 -> 10
    is like <!SYNTAX!><!>field = eq a + 3 == 3 -> 10
    is like <!SYNTAX!><!>field = eq a + 3 && 3 -> 10
    is like <!SYNTAX!><!>field = eq a + 3 || 3 -> 10
    is like val field = eq 3 -> 10
    is like val field = eq a + 3 -> 10
    is like val field = eq a + 3 > 3 -> 10
    is like val field = eq a + 3 == 3 -> 10
    is like val field = eq a + 3 && 3 -> 10
    is like val field = eq a + 3 || 3 -> 10
    is like val field = 3 -> 10
    is like val field = a + 3 -> 10
    is like val field = a + 3 > 3 -> 10
    is like val field = a + 3 == 3 -> 10
    is like val field = a + 3 && 3 -> 10
    is like val field = a + 3 || 3 -> 10
    is like eq a + 3 -> 10
    is like eq a + 3 > 3 -> 10
    is like eq a + 3 == 3 -> 10
    is like eq a + 3 && 3 -> 10
    is like eq a + 3 || 3 -> 10
    is like eq (a + 3) -> 10
    is like eq (a + 3 > 3) -> 10
    is like eq (a + 3 == 3) -> 10
    is like eq (a + 3 && 3) -> 10
    is like eq (a + 3 || 3) -> 10
}