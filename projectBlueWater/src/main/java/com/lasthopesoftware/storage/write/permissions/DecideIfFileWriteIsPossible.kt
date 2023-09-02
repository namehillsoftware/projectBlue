package com.lasthopesoftware.storage.write.permissions

import java.io.File

interface DecideIfFileWriteIsPossible {
    fun isFileWritePossible(file: File): Boolean
}
