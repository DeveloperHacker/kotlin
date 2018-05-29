
data class Pair<F, S>(val first: F, val second: S)

inline deconstructor fun <reified F, reified S> Any.Pair() = when (<!DEBUG_INFO_SMARTCAST!>this<!>) {
    !is Pair<*, *> -> null
    is like (val f is F, val s is S) -> Pair<F, S>(<!DEBUG_INFO_SMARTCAST!>f<!>, <!DEBUG_INFO_SMARTCAST!>s<!>)
    else -> null
}

fun foo(a: Any) = when (a) {
    is like Pair<Int, Int>(val f, val s) -> f + s
    else -> 10
}
