package com.lasthopesoftware.storage.read.permissions

import com.lasthopesoftware.storage.recursivelyTestIfFileExists
import java.io.File

class FileReadPossibleArbitrator : IFileReadPossibleArbitrator {
	override fun isFileReadPossible(file: File): Boolean = file.recursivelyTestIfFileExists { f -> f.canRead() }
}
