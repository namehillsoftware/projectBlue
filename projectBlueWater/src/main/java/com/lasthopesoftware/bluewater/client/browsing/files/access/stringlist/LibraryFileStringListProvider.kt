package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryFileStringListProvider(private val libraryAccess: ProvideRemoteLibraryAccess) :
	ProvideFileStringListsForParameters
{

	override fun promiseFileStringList(libraryId: LibraryId, option: FileListParameters.Options, vararg params: String): Promise<String> =
		libraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { connection ->
				connection
					?.promiseFileStringList(option, *params)
					.keepPromise("")
			}
}
