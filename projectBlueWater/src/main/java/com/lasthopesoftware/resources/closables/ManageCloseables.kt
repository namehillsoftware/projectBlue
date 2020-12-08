package com.lasthopesoftware.resources.closables

interface ManageCloseables : AutoCloseable {
	fun manage(closeable: AutoCloseable)
}
