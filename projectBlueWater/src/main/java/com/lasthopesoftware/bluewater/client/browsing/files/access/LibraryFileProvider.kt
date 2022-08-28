package com.lasthopesoftware.bluewater.client.browsing.files.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class LibraryFileProvider(private val fileStringListProvider: LibraryFileStringListProvider) : ProvideLibraryFiles {
	override fun promiseFiles(libraryId: LibraryId, option: FileListParameters.Options, vararg params: String): Promise<List<ServiceFile>> {
		return fileStringListProvider
			.promiseFileStringList(libraryId, option, *params)
			.eventually(FileResponses)
			.then(FileResponses)
	}
}
