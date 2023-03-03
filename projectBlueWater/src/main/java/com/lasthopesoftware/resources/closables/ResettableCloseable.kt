package com.lasthopesoftware.resources.closables

interface ResettableCloseable : AutoCloseable {
	fun reset()
}
