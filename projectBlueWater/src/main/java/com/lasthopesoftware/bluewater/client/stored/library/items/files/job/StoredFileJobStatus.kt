package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import java.io.File

class StoredFileJobStatus(
    val downloadedFile: File,
    val storedFile: StoredFile,
    val storedFileJobState: StoredFileJobState
) {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as StoredFileJobStatus
        return storedFile == that.storedFile && storedFileJobState == that.storedFileJobState
    }

    override fun hashCode(): Int {
        var result = storedFile.hashCode()
        result = 31 * result + storedFileJobState.hashCode()
        return result
    }
}
