package com.lasthopesoftware.resources.closables

interface ManagePromisingCloseables : ManageCloseables {
	fun <T : PromisingCloseable> manage(closeable: T): T
}
