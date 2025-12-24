package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.resources.io.PromisingWritableStream
import com.namehillsoftware.handoff.promises.Promise

interface ProduceStoredFileDestinations {

	fun promiseOutputStream(storedFile: StoredFile): Promise<PromisingWritableStream?>
}
