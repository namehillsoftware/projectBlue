package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.connection.session.ProvideSelectedConnection
import com.namehillsoftware.handoff.promises.Promise

class LibraryViewsProvider(private val selectedConnection: ProvideSelectedConnection, private val libraryViewsUsingConnection: ProvideLibraryViewsUsingConnection) : ProvideLibraryViews {
	override fun promiseLibraryViews(): Promise<Collection<ViewItem>> {
		return selectedConnection
			.promiseSessionConnection()
			.eventually { libraryViewsUsingConnection.promiseLibraryViewsFromConnection(it) }
	}
}
