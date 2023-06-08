package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.storage.read.exceptions.StorageReadFileException
import java.io.File

/**
 * Created by david on 7/17/16.
 */
class StoredFileReadException @JvmOverloads constructor(
    file: File,
    override val storedFile: StoredFile,
    innerException: Exception? = null
) : StorageReadFileException(file, innerException), IStoredFileJobException
