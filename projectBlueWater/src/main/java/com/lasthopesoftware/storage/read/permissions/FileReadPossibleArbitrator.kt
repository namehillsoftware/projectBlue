package com.lasthopesoftware.storage.read.permissions

import com.lasthopesoftware.storage.walkUpUntilFileExists
import java.io.File

class FileReadPossibleArbitrator : IFileReadPossibleArbitrator {
	override fun isFileReadPossible(file: File): Boolean = file.walkUpUntilFileExists()?.canRead() ?: false
}
