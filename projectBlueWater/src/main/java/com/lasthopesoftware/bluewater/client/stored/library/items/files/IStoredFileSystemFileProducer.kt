package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import java.io.File

interface IStoredFileSystemFileProducer {
    fun getFile(storedFile: StoredFile): File
}
