package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import java.io.File

class StoredFileSystemFileProducer : IStoredFileSystemFileProducer {
    override fun getFile(storedFile: StoredFile): File? {
        return storedFile.path?.let(::File)
    }
}
