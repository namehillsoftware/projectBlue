package com.lasthopesoftware.bluewater.client.stored.library.items.files.job

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile

data class StoredFileJob(val libraryId: LibraryId, val serviceFile: ServiceFile, val storedFile: StoredFile)
