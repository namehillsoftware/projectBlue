package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile

interface IStoredFileJobException {
    val storedFile: StoredFile
}
