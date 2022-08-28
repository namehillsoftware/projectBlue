package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ItemStringListProvider(
	private val parameterProvider: IFileListParameterProvider,
	private val parameterizedFileStringListProvider: ProvideFileStringListsForParameters
) : ProvideFileStringListForItem {
	override fun promiseFileStringList(libraryId: LibraryId, itemId: ItemId, options: FileListParameters.Options): Promise<String> {
		// Put any crazy workarounds to get a fresh file list in here
		val parameters = parameterProvider.getFileListParameters(itemId)
		return parameterizedFileStringListProvider.promiseFileStringList(libraryId, options, *parameters)
	}
}
