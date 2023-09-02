package com.lasthopesoftware.resources.io

import java.io.File
import java.net.URI

interface SupplyFiles {
	fun getFile(path: String): File

	fun getFile(uri: URI): File

	fun getFile(parent: File, child: String): File
}
