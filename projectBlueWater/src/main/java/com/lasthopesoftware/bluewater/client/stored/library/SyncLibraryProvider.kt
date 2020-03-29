package com.lasthopesoftware.bluewater.client.stored.library

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class SyncLibraryProvider(private val libraryProvider: ILibraryProvider) : ILibraryProvider {
	override fun getAllLibraries(): Promise<Collection<Library>> {
		return libraryProvider.allLibraries.then { l -> l.map { it.transformConnectionSetting() } }
	}

	override fun getLibrary(libraryId: LibraryId?): Promise<Library> {
		return libraryProvider.getLibrary(libraryId).then {
			it.transformConnectionSetting()
		}
	}

	private fun Library.transformConnectionSetting(): Library {
		return this.setLocalOnly(this.isSyncLocalConnectionsOnly)
	}
}
