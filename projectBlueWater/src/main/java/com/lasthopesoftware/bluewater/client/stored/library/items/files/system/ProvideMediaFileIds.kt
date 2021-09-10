package com.lasthopesoftware.bluewater.client.stored.library.items.files.system

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideMediaFileIds {
	fun getMediaId(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Int>
}
