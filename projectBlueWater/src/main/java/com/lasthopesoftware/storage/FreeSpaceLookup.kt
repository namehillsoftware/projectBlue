package com.lasthopesoftware.storage

import java.io.File

object FreeSpaceLookup : GetFreeSpace {
	override fun getFreeSpace(file: File): Long  = file.freeSpace
}
