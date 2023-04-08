package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile

class StoredFileJobException(override val storedFile: StoredFile, innerException: Throwable?) :
    Exception(innerException), IStoredFileJobException
