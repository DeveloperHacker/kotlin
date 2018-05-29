
data class Pair<F, S>(val first: F, val second: S)

fun Int.isOdd() = this % 2 == 1

fun Int.isEven() = this % 2 == 0

fun foo(any: Any) = when (<!DEBUG_INFO_SMARTCAST!>any<!>) {
    is like Pair(val a is Int, val b is Int) && <!DEBUG_INFO_SMARTCAST!>a<!>.isOdd() == <!DEBUG_INFO_SMARTCAST!>b<!>.isOdd() -> 1
    is like Pair(val a is Int, val b is Int) && <!DEBUG_INFO_SMARTCAST!>a<!>.isOdd() && <!DEBUG_INFO_SMARTCAST!>b<!>.isOdd() -> 2
    is like Pair(val a is Int, val b is Int) && (<!DEBUG_INFO_SMARTCAST!>a<!>.isOdd() || <!DEBUG_INFO_SMARTCAST!>b<!>.isOdd()) -> 3
    is like Pair(val a is Int, val b is Int) && <!EXPECTED_PARENTHESISE_GUARD!><!DEBUG_INFO_SMARTCAST!>a<!>.isEven() || <!DEBUG_INFO_SMARTCAST!>b<!>.isEven()<!> -> 4
    else -> 0
}
