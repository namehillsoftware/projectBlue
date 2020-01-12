package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryFiles {
	fun promiseFiles(libraryId: LibraryId, option: FileListParameters.Options, vararg params: String): Promise<List<ServiceFile>>
}
