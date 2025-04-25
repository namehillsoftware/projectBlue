package com.lasthopesoftware.bluewater.shared

import java.util.concurrent.atomic.AtomicReference

fun <T> AtomicReference<T>.updateIfDifferent(newValue: T): Boolean {
	return updateConditionally({ it != newValue }) { newValue }
}

inline fun <T> AtomicReference<T>.updateConditionally(test: (T) -> Boolean, factory: (T) -> T): Boolean {
	do {
		val prev = get()
		if (!test(prev)) return false
	} while (!compareAndSet(prev, factory(prev)))

	return true
}

inline fun <T> AtomicReference<T>.update(factory: (T) -> T) {
	while (!tryUpdate(factory)) {}
}

inline fun <T> AtomicReference<T>.tryUpdate(factory: (T) -> T): Boolean {
	val prev = get()
	return compareAndSet(prev, factory(prev))
}
