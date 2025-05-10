package com.lasthopesoftware.bluewater.shared

interface Gate {
	fun open(): AutoCloseable
}
