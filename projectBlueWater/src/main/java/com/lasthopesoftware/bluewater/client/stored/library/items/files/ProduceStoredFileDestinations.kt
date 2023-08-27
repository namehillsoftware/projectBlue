package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.namehillsoftware.handoff.promises.Promise
import java.io.OutputStream

interface ProduceStoredFileDestinations {

	fun promiseOutputStream(storedFile: StoredFile): Promise<OutputStream?>
}
