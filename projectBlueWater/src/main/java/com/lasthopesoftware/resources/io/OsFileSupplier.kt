package com.lasthopesoftware.resources.io

import java.io.File
import java.net.URI

object OsFileSupplier : SupplyFiles {
	override fun getFile(path: String): File = File(path)

	override fun getFile(uri: URI): File = File(uri)

	override fun getFile(parent: File, child: String): File = File(parent, child)
}
