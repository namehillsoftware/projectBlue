package com.lasthopesoftware.bluewater.shared

import java.util.concurrent.atomic.AtomicReference

fun <T> AtomicReference<T>.updateIfDifferent(newValue: T): Boolean {
	return updateOnFail({ it == newValue }) { newValue }
}

inline fun <T> AtomicReference<T>.updateOnFail(test: (T) -> Boolean, factory: (T) -> T): Boolean {
	do {
		val prev = get()
		if (test(prev)) return false
	} while (!compareAndSet(prev, factory(prev)))

	return true
}
