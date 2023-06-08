package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile

class StoredFileJobStatus(
	val storedFile: StoredFile,
    val storedFileJobState: StoredFileJobState
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as StoredFileJobStatus
        return storedFile == that.storedFile && storedFileJobState == that.storedFileJobState
    }

    override fun hashCode(): Int {
        var result = storedFile.hashCode()
        result = 31 * result + storedFileJobState.hashCode()
        return result
    }
}
