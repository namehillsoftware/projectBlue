package com.lasthopesoftware.resources.closables

interface ManagePromisingCloseables {
	fun <T : PromisingCloseable> manage(closeable: T): T
}
