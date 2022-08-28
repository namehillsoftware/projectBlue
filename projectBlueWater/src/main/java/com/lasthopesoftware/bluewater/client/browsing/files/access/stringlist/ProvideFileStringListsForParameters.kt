package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideFileStringListsForParameters {
	fun promiseFileStringList(libraryId: LibraryId, option: FileListParameters.Options, vararg params: String): Promise<String>
}
