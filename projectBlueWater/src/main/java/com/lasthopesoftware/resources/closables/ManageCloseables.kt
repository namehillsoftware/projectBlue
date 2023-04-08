package com.lasthopesoftware.resources.closables

interface ManageCloseables {
	fun <T : AutoCloseable> manage(closeable: T): T
}
