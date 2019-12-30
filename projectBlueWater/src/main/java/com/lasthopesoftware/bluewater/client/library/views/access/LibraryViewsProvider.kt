package com.lasthopesoftware.bluewater.client.library.views.access

import com.lasthopesoftware.bluewater.client.connection.session.ProvideSessionConnection
import com.lasthopesoftware.bluewater.client.library.views.ViewItem
import com.namehillsoftware.handoff.promises.Promise

class LibraryViewsProvider(private val sessionConnection: ProvideSessionConnection, private val libraryViewsUsingConnection: ProvideLibraryViewsUsingConnection) : ProvideLibraryViews {
	override fun promiseLibraryViews(): Promise<Collection<ViewItem>> {
		return sessionConnection
			.promiseSessionConnection()
			.eventually { libraryViewsUsingConnection.promiseLibraryViewsFromConnection(it) }
	}
}
