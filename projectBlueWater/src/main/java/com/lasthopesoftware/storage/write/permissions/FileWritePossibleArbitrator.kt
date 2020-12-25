package com.lasthopesoftware.storage.write.permissions

import com.lasthopesoftware.storage.walkUpUntilFileExists
import java.io.File

class FileWritePossibleArbitrator : IFileWritePossibleArbitrator {
	override fun isFileWritePossible(file: File): Boolean {
		val firstFileThatExists = file.walkUpUntilFileExists()
		return firstFileThatExists?.canWrite() ?: false
	}
}
