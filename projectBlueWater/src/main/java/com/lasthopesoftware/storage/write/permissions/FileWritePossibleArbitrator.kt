package com.lasthopesoftware.storage.write.permissions

import com.lasthopesoftware.storage.recursivelyTestIfFileExists
import java.io.File

class FileWritePossibleArbitrator : IFileWritePossibleArbitrator {
	override fun isFileWritePossible(file: File): Boolean = file.recursivelyTestIfFileExists { f -> f.canWrite() }
}
