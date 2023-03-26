package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideFileStringListForItem {
	fun promiseFileStringList(libraryId: LibraryId, itemId: ItemId? = null, options: FileListParameters.Options = FileListParameters.Options.None): Promise<String>
}
