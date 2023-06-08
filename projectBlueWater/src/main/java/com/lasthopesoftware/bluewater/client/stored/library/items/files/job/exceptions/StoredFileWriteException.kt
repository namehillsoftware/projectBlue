package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.storage.write.exceptions.StorageWriteFileException
import java.io.File

/**
 * Created by david on 7/17/16.
 */
class StoredFileWriteException @JvmOverloads constructor(
    file: File,
    override val storedFile: StoredFile,
    innerException: Exception? = null
) : StorageWriteFileException(file, innerException), IStoredFileJobException
