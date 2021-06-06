package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class PassThroughLibraryStorage : ILibraryStorage {
	override fun saveLibrary(library: Library): Promise<Library> = library.toPromise()

	override fun removeLibrary(library: Library): Promise<Unit> = Unit.toPromise()
}
