package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile

data class StoredFileJobStatus(
	val storedFile: StoredFile,
    val storedFileJobState: StoredFileJobState
)
