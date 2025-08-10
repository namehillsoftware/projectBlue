package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface LoadFileDetailsState {
	fun load(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit>
}
