package com.lasthopesoftware.storage

import java.io.File

fun File.walkUpUntilFileExists(): File? {
	var testFile: File? = this
	while (testFile != null && !testFile.exists()) {
		testFile = testFile.parentFile
	}

	return testFile
}
