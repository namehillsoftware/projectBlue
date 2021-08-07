package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemResponse
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryViewsProvider(private val selectedConnection: ProvideSelectedConnection, private val checkScopedRevisions: CheckScopedRevisions) : ProvideLibraryViews {
	companion object {
		const val browseLibraryParameter = "Browse/Children"
	}

	private var cachedFileSystemItems: Collection<ViewItem>? = null
	private var revision: Int? = null

	override fun promiseLibraryViews(): Promise<Collection<ViewItem>> =
		selectedConnection
			.promiseSessionConnection()
			.eventually(::promiseLibraryViewsFromConnection)

	private fun promiseLibraryViewsFromConnection(connectionProvider: IConnectionProvider?): Promise<Collection<ViewItem>> {
		connectionProvider ?: return Promise(emptySet())

		return checkScopedRevisions.promiseRevision()
			.eventually { serverRevision ->
				synchronized(browseLibraryParameter) {
					cachedFileSystemItems?.takeIf { revision == serverRevision }?.toPromise()
				} ?: connectionProvider.promiseResponse(browseLibraryParameter)
					.then { response ->
						response.body?.use { b ->
							val viewItems = b.byteStream().use(ItemResponse::GetItems)
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
						} ?: emptySet()
					}
			}
	}
}
