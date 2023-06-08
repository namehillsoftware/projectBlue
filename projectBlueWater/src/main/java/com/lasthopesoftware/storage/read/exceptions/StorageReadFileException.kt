package com.lasthopesoftware.storage.read.exceptions

import java.io.File
import java.io.IOException

open class StorageReadFileException @JvmOverloads constructor(
    val file: File,
    innerException: Exception? = null
) : IOException(
    "There was an error reading the file $file.", innerException
)
