package com.lasthopesoftware.bluewater.shared

import java.util.concurrent.atomic.AtomicReference

fun <T> AtomicReference<T>.updateIfDifferent(newValue: T): Boolean {
	return updateConditionally({ it != newValue }) { newValue }
}

inline fun <T> AtomicReference<T>.updateConditionallyWithNext(test: (T, T) -> Boolean, factory: (T) -> T): Boolean {
	do {
		val prev = get()
		val next = factory(prev)
		if (!test(prev, next)) return false
	} while (!compareAndSet(prev, next))

	return true
}

inline fun <T> AtomicReference<T>.updateConditionally(test: (T) -> Boolean, factory: (T) -> T): Boolean {
	do {
		val prev = get()
		if (!test(prev)) return false
	} while (!compareAndSet(prev, factory(prev)))

	return true
}
