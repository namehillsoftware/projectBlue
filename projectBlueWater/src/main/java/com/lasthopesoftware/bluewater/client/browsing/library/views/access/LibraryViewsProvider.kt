package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemResponse
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.namehillsoftware.handoff.promises.Promise

class LibraryViewsProvider(private val libraryConnectionProvider: ProvideLibraryConnections) : ProvideLibraryViews {
	companion object {
		const val browseLibraryParameter = "Browse/Children"
	}

	override fun promiseLibraryViews(libraryId: LibraryId): Promise<Collection<ViewItem>> =
		libraryConnectionProvider
			.promiseLibraryConnection(libraryId)
			.eventually { c ->
				c?.promiseResponse(browseLibraryParameter)
					?.then { response ->
						response.body?.use { b ->
							b.byteStream()
								.use(ItemResponse::GetItems)
								.map { i ->
									when (i.value) {
										KnownViews.Playlists -> PlaylistViewItem(i.key)
										else -> StandardViewItem(i.key, i.value)
									}
								}

						} ?: emptySet()
					}
					?: Promise(emptySet())
			}
}
