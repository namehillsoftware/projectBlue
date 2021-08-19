package com.lasthopesoftware.storage

import java.io.File

fun interface GetFreeSpace {
	fun getFreeSpace(file: File): Long
}
