data class Pair<F, S>(val first: F, val second: S)

class A<F, S>(val a: F, val b: S) {
    deconstructor fun SameValues() = if (a == b) Pair(a, b) else null
}

deconstructor fun <F, S> A<F?, S?>.NotNull() = if (a != null && b != null) Pair(<!DEBUG_INFO_SMARTCAST!>a<!>, <!DEBUG_INFO_SMARTCAST!>b<!>) else null

deconstructor fun <F, S> A<F, S>.Unapply() = Pair(a, b)

fun <F, S>foo(a: A<F?, S?>) = when (a) {
    is like SameValues(val f, val s) -> f == s
    is like NotNull(val f, val s) -> f != s
    is like Unapply(val f, val s) -> f == s
}