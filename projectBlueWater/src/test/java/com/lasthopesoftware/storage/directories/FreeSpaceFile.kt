package com.lasthopesoftware.storage.directories

import java.io.File

internal class FreeSpaceFile(private val pathname: String, private val freeSpace: Long) : File(
	pathname
) {
	override fun getPath(): String = pathname

	override fun getFreeSpace(): Long = freeSpace
}
