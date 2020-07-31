package com.lasthopesoftware.storage

import java.io.File

interface GetFreeSpace {
	fun getFreeSpace(file: File): Long
}
