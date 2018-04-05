data class Pair<F, S>(val first: F, val second: S)

class A<T>(val value: T) {
    operator fun component1() = value

    deconstructor fun AOfInt(): A<Int>? = if (value is Int) this <!UNCHECKED_CAST!>as A<Int><!> else null

    deconstructor fun AOfFloat(): A<Float>? = if (value is Float) this <!UNCHECKED_CAST!>as A<Float><!> else null
}

deconstructor fun Any?.AOfInt(): A<Int>? = if (<!DEBUG_INFO_SMARTCAST!>this<!> is like A(Int())) this <!UNCHECKED_CAST!>as A<Int><!> else null

fun Any?.AOfFloat(): A<Float>? = if (<!DEBUG_INFO_SMARTCAST!>this<!> is like A(Float())) this <!UNCHECKED_CAST!>as A<Float><!> else null

deconstructor fun Any?.PairOfInt(): Pair<Int, Int>? = if (<!DEBUG_INFO_SMARTCAST!>this<!> is like Pair(Int(), Int())) this <!UNCHECKED_CAST!>as Pair<Int, Int><!> else null

fun Any?.PairOfFloat(): Pair<Float, Float>? = if (<!DEBUG_INFO_SMARTCAST!>this<!> is like Pair(Float(), Float())) this <!UNCHECKED_CAST!>as Pair<Float, Float><!> else null

fun foo1(a: Any?) = when(a) {
    is like AOfInt(val value) -> value * 2
    is like AOfInt(a is Any) -> 2
    is like <!DECONSTRUCTOR_MODIFIER_REQUIRED!>AOfFloat<!>(val value) -> value.toInt() * 2
    is like PairOfInt(val first, val second) -> first * second
    is like <!DECONSTRUCTOR_MODIFIER_REQUIRED!>PairOfFloat<!>(val first, val second) -> (first * second).toInt()
    else -> 0
}

fun foo2(a: A<*>) = when(a) {
    is like AOfInt(val value) -> value * 2
    is like AOfFloat(val value) -> value.toInt() * 2
    is like PairOfInt(val first, val second) -> first * second
    is like <!DECONSTRUCTOR_MODIFIER_REQUIRED!>PairOfFloat<!>(val first, val second) -> (first * second).toInt()
    else -> 0
}
