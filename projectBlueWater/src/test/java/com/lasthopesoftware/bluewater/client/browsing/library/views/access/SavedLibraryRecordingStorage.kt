package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise

class SavedLibraryRecordingStorage : PassThroughLibraryStorage() {
	var savedLibrary: Library? = null

	override fun saveLibrary(library: Library): Promise<Library> {
		savedLibrary = library
		return super.saveLibrary(library)
	}
}
