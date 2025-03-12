package com.lasthopesoftware.bluewater.client.browsing.library.revisions

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryRevisionProvider(
	private val libraryAccess: ProvideRemoteLibraryAccess,
	private val libraryConnections: ProvideLibraryConnections,
	private val serverRevisionData: HasServerRevisionData,
) : CheckRevisions {
	companion object {
		private const val badRevision = -1
	}

	override fun promiseRevision(libraryId: LibraryId): Promise<Int> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.cancelBackEventually { connection ->
				connection?.urlProvider?.baseUrl?.let { url ->
					serverRevisionData.getOrSetRevisionData(url) {
						libraryAccess
							.promiseLibraryAccess(libraryId)
							.cancelBackEventually { it?.promiseRevision().keepPromise(badRevision) }
					}
				}.keepPromise(badRevision)
			}
			.then(
				forward(),
				{ badRevision }
			)
}
