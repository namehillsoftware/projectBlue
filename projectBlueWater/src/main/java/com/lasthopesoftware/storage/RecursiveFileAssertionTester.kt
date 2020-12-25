package com.lasthopesoftware.storage

import java.io.File

fun File.walkUpUntilFileExists(): File? {
	var testFile = this
	do {
		if (testFile.exists()) return testFile
	} while (testFile.parentFile?.also { testFile = it } != null)

	return null
}
