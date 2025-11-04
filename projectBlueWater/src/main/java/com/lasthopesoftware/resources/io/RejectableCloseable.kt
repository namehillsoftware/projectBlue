package com.lasthopesoftware.resources.io

interface RejectableCloseable {
	fun closeWithCause(reason: Throwable)
}
