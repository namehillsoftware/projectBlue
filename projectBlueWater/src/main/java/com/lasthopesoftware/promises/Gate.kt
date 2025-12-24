package com.lasthopesoftware.promises

interface Gate {
	fun open(): AutoCloseable
}
