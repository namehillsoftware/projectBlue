package com.lasthopesoftware.storage.read.permissions

import java.io.File

interface IFileReadPossibleArbitrator {
    fun isFileReadPossible(file: File): Boolean
}
