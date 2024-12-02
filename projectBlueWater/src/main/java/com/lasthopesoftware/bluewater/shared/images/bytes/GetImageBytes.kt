package com.lasthopesoftware.bluewater.shared.images.bytes

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface GetImageBytes {
	fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray>

	fun promiseImageBytes(libraryId: LibraryId, itemId: ItemId): Promise<ByteArray>
}
