package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemResponse
import com.lasthopesoftware.bluewater.client.browsing.library.access.RevisionChecker
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryViewsByConnectionProvider : ProvideLibraryViewsUsingConnection {
	private var cachedFileSystemItems: Collection<ViewItem>? = null
	private var revision: Int? = null

	override fun promiseLibraryViewsFromConnection(connectionProvider: IConnectionProvider): Promise<Collection<ViewItem>> {
		return RevisionChecker.promiseRevision(connectionProvider)
			.eventually { serverRevision ->
				synchronized(browseLibraryParameter) {
					cachedFileSystemItems?.takeIf { revision == serverRevision }?.let { it.toPromise() }
				} ?: connectionProvider.promiseResponse(browseLibraryParameter)
				.then { response ->
					response.body?.use { b ->
						b.byteStream().use { s ->
							val viewItems = ItemResponse.GetItems(s)
								.map { i ->
									when (i.value) {
										KnownViews.Playlists -> PlaylistViewItem(i.key)
										else -> StandardViewItem(i.key, i.value)
									}
								}

							synchronized(browseLibraryParameter) {
								revision = serverRevision
								cachedFileSystemItems = viewItems
							}

							viewItems
						}
					} ?: emptySet()
				}
			}
	}

	companion object {
		const val browseLibraryParameter = "Browse/Children"
	}
}
