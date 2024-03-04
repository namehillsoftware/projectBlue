package com.lasthopesoftware.bluewater.client.stored.library

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class SyncLibraryProvider(private val libraryProvider: ILibraryProvider) : ILibraryProvider {
	override val allLibraries: Promise<Collection<Library>>
		get() = libraryProvider.allLibraries.then { l -> l.map { it.transformConnectionSetting() } }

	override fun promiseLibrary(libraryId: LibraryId): Promise<Library?> =
		libraryProvider.promiseLibrary(libraryId).then { it -> it?.transformConnectionSetting() }

	private fun Library.transformConnectionSetting() = copy(isLocalOnly = isSyncLocalConnectionsOnly)
}
