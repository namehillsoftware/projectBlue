package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideItemFiles {
	fun promiseFiles(libraryId: LibraryId, itemId: ItemId, options: FileListParameters.Options): Promise<List<ServiceFile>>
}
