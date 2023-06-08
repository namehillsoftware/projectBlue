package com.lasthopesoftware.storage.write.permissions

import java.io.File

interface IFileWritePossibleArbitrator {
    fun isFileWritePossible(file: File): Boolean
}
