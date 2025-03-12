package com.lasthopesoftware.bluewater.client.servers.version

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryServerVersionProvider(private val remoteLibraryAccess: ProvideRemoteLibraryAccess) : ProvideLibraryServerVersion {
	override fun promiseServerVersion(libraryId: LibraryId): Promise<SemanticVersion?> =
		remoteLibraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { it?.promiseServerVersion().keepPromise() }
}
