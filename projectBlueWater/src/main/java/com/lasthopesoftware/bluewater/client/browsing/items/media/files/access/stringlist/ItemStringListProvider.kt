package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideFreshItems
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ItemStringListProvider(
	private val itemProvider: ProvideFreshItems,
	private val parameterProvider: IFileListParameterProvider,
	private val parameterizedFileStringListProvider: ProvideFileStringListsForParameters
) : ProvideFileStringListForItem {
	override fun promiseFileStringList(libraryId: LibraryId, itemId: ItemId, options: FileListParameters.Options): Promise<String> {
		val parameters = parameterProvider.getFileListParameters(itemId)
		return itemProvider.promiseItems(libraryId, itemId)
			.eventually {
				parameterizedFileStringListProvider.promiseFileStringList(libraryId, options, *parameters)
			}
			.eventually { itemProvider.promiseItems(libraryId, itemId) }
			.eventually {
				parameterizedFileStringListProvider.promiseFileStringList(libraryId, options, *parameters)
			}
	}
}
