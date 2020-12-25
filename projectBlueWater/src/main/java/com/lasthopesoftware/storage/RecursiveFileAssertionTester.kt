package com.lasthopesoftware.storage

import java.io.File

fun File.recursivelyTestIfFileExists(assertion: (File) -> Boolean): Boolean {
	var testFile = this
	do {
		if (testFile.exists()) return assertion(testFile)
	} while (testFile.parentFile?.also { testFile = it } != null)

	return false
}
