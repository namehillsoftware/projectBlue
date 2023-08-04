package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileId
import com.namehillsoftware.handoff.promises.Promise
import java.io.File
import java.io.OutputStream

class StoredFileUriProducer : ProduceStoredFileDestinations {
    override fun getFile(storedFile: StoredFile): File? {
        return storedFile.path?.let(::File)
    }

	override fun promiseOutputStream(storedFileId: StoredFileId): Promise<OutputStream?> {
		TODO("Not yet implemented")
	}
}
