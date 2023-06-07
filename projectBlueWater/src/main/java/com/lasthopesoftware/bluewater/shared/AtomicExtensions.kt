package com.lasthopesoftware.bluewater.shared

import java.util.concurrent.atomic.AtomicReference

fun <T> AtomicReference<T>.updateIfDifferent(newValue: T): Boolean {
	do {
		val prev = get()
		if (prev == newValue) return false
	} while (!compareAndSet(prev, newValue))

	return true
}
