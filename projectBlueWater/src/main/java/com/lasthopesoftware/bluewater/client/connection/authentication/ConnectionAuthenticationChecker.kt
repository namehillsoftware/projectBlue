package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ConnectionAuthenticationChecker(private val libraryAccess: ProvideRemoteLibraryAccess) : CheckIfConnectionIsReadOnly {
	override fun promiseIsReadOnly(libraryId: LibraryId): Promise<Boolean> = libraryAccess
		.promiseLibraryAccess(libraryId)
		.cancelBackEventually { it?.promiseIsReadOnly().keepPromise(false) }
}
