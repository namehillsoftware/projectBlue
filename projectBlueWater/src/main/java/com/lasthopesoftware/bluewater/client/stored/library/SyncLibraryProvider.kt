package com.lasthopesoftware.bluewater.client.stored.library

import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.library.repository.Library
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class SyncLibraryProvider(private val libraryProvider: ILibraryProvider) : ILibraryProvider {
	override fun getAllLibraries(): Promise<MutableCollection<Library>> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getLibrary(libraryId: Int): Promise<Library> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getLibrary(libraryId: LibraryId?): Promise<Library> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}
