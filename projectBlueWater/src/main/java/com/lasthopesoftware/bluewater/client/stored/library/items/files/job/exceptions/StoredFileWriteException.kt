package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.storage.write.exceptions.StorageWriteFileException
import java.io.File

class StoredFileWriteException(
    override val storedFile: StoredFile,
	file: File? = null,
    innerException: Exception? = null
) : StorageWriteFileException(file, innerException), IStoredFileJobException
