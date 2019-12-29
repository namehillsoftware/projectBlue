package com.lasthopesoftware.bluewater.client.library.views.access

import com.lasthopesoftware.bluewater.client.connection.session.ProvideSessionConnection
import com.lasthopesoftware.bluewater.client.library.items.Item
import com.namehillsoftware.handoff.promises.Promise

class LibraryViewsProvider(private val sessionConnection: ProvideSessionConnection, private val libraryViewsUsingConnection: ProvideLibraryViewsUsingConnection) : ProvideLibraryViews {
	override fun promiseLibraryViews(): Promise<List<Item>> {
		return sessionConnection
			.promiseSessionConnection()
			.eventually { libraryViewsUsingConnection.promiseLibraryViewsFromConnection(it) }
	}
}
