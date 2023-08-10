package com.lasthopesoftware.bluewater.client.stored.library.items.files

import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import java.io.OutputStream

interface ProduceStoredFileDestinations {

	fun getOutputStream(storedFile: StoredFile): OutputStream?
}
