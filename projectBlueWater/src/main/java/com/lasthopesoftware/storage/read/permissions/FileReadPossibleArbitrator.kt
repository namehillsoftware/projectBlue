package com.lasthopesoftware.storage.read.permissions

import com.lasthopesoftware.storage.recursivelyTestWhenFileExists
import java.io.File

class FileReadPossibleArbitrator : IFileReadPossibleArbitrator {
	override fun isFileReadPossible(file: File): Boolean = file.recursivelyTestWhenFileExists { f -> f.canRead() }
}
