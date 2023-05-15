package com.lasthopesoftware.bluewater.shared

import java.util.Queue

fun <T> Queue<T>.drainQueue(): Iterable<T> {
	val queue = this
	return Iterable {
		iterator {
			do {
				val next = queue.poll()
				if (next != null)
					yield(next)
			} while (next != null)
		}
	}
}
