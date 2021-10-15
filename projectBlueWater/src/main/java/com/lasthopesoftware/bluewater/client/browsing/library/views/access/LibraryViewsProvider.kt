package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemResponse
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryViewsProvider(private val libraryConnectionProvider: LibraryConnectionProvider, private val revisions: CheckRevisions) : ProvideLibraryViews {
	companion object {
		const val browseLibraryParameter = "Browse/Children"
	}

	private var cachedFileSystemItems: Collection<ViewItem>? = null
	private var revision: Int? = null

	override fun promiseLibraryViews(libraryId: LibraryId): Promise<Collection<ViewItem>> =
		revisions.promiseRevision(libraryId)
			.eventually { serverRevision ->
				synchronized(browseLibraryParameter) {
					cachedFileSystemItems?.takeIf { revision == serverRevision }?.toPromise()
				} ?: libraryConnectionProvider.promiseLibraryConnection(libraryId).eventually { c ->
					c?.promiseResponse(browseLibraryParameter)
						?.then { response ->
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
						?: Promise(emptySet())
				}
			}
}
