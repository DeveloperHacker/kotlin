class Equlitive(val value: Int) {
    operator fun component1() = value

    infix fun eq(other: Equlitive) = Equlitive(value + other.value)
    
    infix fun eq(other: Int) = value + other + 21

    override fun equals(other: Any?) = when (<!DEBUG_INFO_SMARTCAST!>other<!>) {
        is like Equlitive(val o) -> o == value
        is like val o is Int -> o == value + 5
        else -> false
    }
}

fun foo(a: Equlitive, eq: Int) = when (a) {
    is like (eq) -> 1
    is like (eq eq) -> 2
    is like eq eq -> 3
    !is like (eq) -> 4
    !is like (eq eq) -> 5
    !is like eq eq -> 6
    else -> 0
}

fun foo2(a: Equlitive) = when (a) {
    is like (<!UNRESOLVED_REFERENCE!>eq<!>) -> 1
    is like (eq <!UNRESOLVED_REFERENCE!>eq<!>) -> 2
    is like eq <!UNRESOLVED_REFERENCE!>eq<!> -> 3
    !is like (<!UNRESOLVED_REFERENCE!>eq<!>) -> 4
    !is like (eq <!UNRESOLVED_REFERENCE!>eq<!>) -> 5
    !is like eq <!UNRESOLVED_REFERENCE!>eq<!> -> 6
    else -> 0
}

fun foo3(a: Equlitive, eq: Int, _eq: Equlitive) = when (a) {
    is like (eq) -> 1
    is like (_eq) -> 2
    is like (_eq eq eq) -> 5
    is like (_eq eq _eq) -> 6

    is like (eq eq) -> 3
    is like (eq _eq) -> 4
    is like (eq _eq eq eq) -> 7
    is like (eq _eq eq _eq) -> 8

    is like eq eq -> 13
    is like eq _eq -> 14
    is like eq _eq eq eq -> 17
    is like eq _eq eq _eq -> 18

    !is like (eq) -> -1
    !is like (_eq) -> -2
    !is like (_eq eq eq) -> -5
    !is like (_eq eq _eq) -> -6

    !is like (eq eq) -> -3
    !is like (eq _eq) -> -4
    !is like (eq _eq eq eq) -> -7
    !is like (eq _eq eq _eq) -> -8

    !is like eq eq -> -13
    !is like eq _eq -> -14
    !is like eq _eq eq eq -> -17
    !is like eq _eq eq _eq -> -18
    else -> 0
}
