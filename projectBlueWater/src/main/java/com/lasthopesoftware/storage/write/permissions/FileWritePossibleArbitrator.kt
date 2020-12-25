package com.lasthopesoftware.storage.write.permissions

import com.lasthopesoftware.storage.recursivelyTestWhenFileExists
import java.io.File

class FileWritePossibleArbitrator : IFileWritePossibleArbitrator {
	override fun isFileWritePossible(file: File): Boolean = file.recursivelyTestWhenFileExists { f -> f.canWrite() }
}
