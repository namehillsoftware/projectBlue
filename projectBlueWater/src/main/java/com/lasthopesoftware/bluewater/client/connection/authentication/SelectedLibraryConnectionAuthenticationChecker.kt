package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryConnectionAuthenticationChecker(private val selectedLibraryIdProvider: ProvideSelectedLibraryId, private val connectionPermissions: CheckIfConnectionIsReadOnly) : CheckIfScopedConnectionIsReadOnly {
	override fun promiseIsReadOnly(): Promise<Boolean> =
		selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.eventually { it?.let { l -> connectionPermissions.promiseIsReadOnly(l) }.keepPromise(true) }
}
