package com.lasthopesoftware.resources.closables

interface ManagePromisingCloseables : ManageCloseables, PromisingCloseable {
	fun <T : PromisingCloseable> manage(closeable: T): T

	fun createNestedManager(): ManagePromisingCloseables
}
