package com.lasthopesoftware.bluewater.client.browsing.files.image.bytes

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface GetRawImages {
	fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray>
}
