package com.lasthopesoftware.storage.write.exceptions

import java.io.File
import java.io.IOException

open class StorageWriteFileException @JvmOverloads constructor(
    val file: File,
    innerException: Exception? = null
) : IOException(
    "There was an error writing the file $file.", innerException
)
