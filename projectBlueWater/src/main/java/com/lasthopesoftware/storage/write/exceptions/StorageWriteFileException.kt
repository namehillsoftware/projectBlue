package com.lasthopesoftware.storage.write.exceptions

import java.io.File
import java.io.IOException

open class StorageWriteFileException(
    val file: File? = null,
    innerException: Exception? = null
) : IOException(
	if (file != null) "There was an error writing the file $file."
	else "There was an error writing the file.",
	innerException
)
