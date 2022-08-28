package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.ProvideLibraryViews
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class MediaItemsBrowser(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val itemProvider: ProvideItems,
	private val fileProvider: ProvideLibraryFiles,
	private val itemFileProvider: ProvideItemFiles,
	private val libraryViews: ProvideLibraryViews,
	private val mediaItemServiceFileLookup: GetMediaItemsFromServiceFiles,
) : BrowseMediaItems {
	companion object {
		private fun toMediaItem(item: Item): MediaBrowserCompat.MediaItem =
			MediaBrowserCompat.MediaItem(
				MediaDescriptionCompat
					.Builder()
					.setMediaId(RemoteBrowserService.itemFileMediaIdPrefix + RemoteBrowserService.mediaIdDelimiter + item.key)
					.setTitle(item.value)
					.build(),
				MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
			)
	}

	override fun promiseItems(itemId: ItemId): Promise<Collection<MediaBrowserCompat.MediaItem>> =
		selectedLibraryIdProvider.selectedLibraryId.eventually { maybeId ->
			maybeId
				?.let { libraryId ->
					itemProvider
						.promiseItems(libraryId, itemId)
						.eventually { items ->
							if (items.any()) items.map(::toMediaItem).toPromise()
							else {
								itemFileProvider
									.promiseFiles(libraryId, itemId, FileListParameters.Options.None)
									.eventually<Collection<MediaBrowserCompat.MediaItem>> { files ->
										Promise.whenAll(files.map { f -> mediaItemServiceFileLookup.promiseMediaItem(f).then { mi -> Pair(f, mi) } })
											.then { pairs ->
												val mediaItemsLookup = pairs.associate { p -> p }
												files.mapIndexedNotNull { i, f ->
													mediaItemsLookup[f]?.let { mediaItem ->
														val description = mediaItem.description
														MediaBrowserCompat.MediaItem(
															MediaDescriptionCompat
																.Builder()
																.setMediaId(
																	arrayOf(
																		RemoteBrowserService.itemFileMediaIdPrefix,
																		itemId.id.toString(),
																		i.toString()
																	).joinToString(RemoteBrowserService.mediaIdDelimiter.toString()))
																.setDescription(description.description)
																.setExtras(description.extras)
																.setTitle(description.title)
																.setSubtitle(description.subtitle)
																.build(),
															MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
														)
													}
												}
											}
									}
							}
						}
				}
				.keepPromise(emptyList())
		}

	override fun promiseLibraryItems(): Promise<List<MediaBrowserCompat.MediaItem>> =
		selectedLibraryIdProvider.selectedLibraryId.eventually { maybeId ->
			maybeId
				?.let { libraryId -> libraryViews.promiseLibraryViews(libraryId).then { v -> v.map(::toMediaItem) } }
				.keepPromise(emptyList())
		}

	override fun promiseItems(query: String): Promise<Collection<MediaBrowserCompat.MediaItem>> {
		val parameters = SearchFileParameterProvider.getFileListParameters(query)
		return selectedLibraryIdProvider.selectedLibraryId
			.eventually { maybeId ->
				maybeId
					?.let { libraryId ->
						fileProvider.promiseFiles(libraryId, FileListParameters.Options.None, *parameters)
					}
					.keepPromise(emptyList())
			}
			.eventually { files -> Promise.whenAll(files.map(mediaItemServiceFileLookup::promiseMediaItem)) }
	}
}
