package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class MediaItemsBrowser(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val itemProvider: ProvideItems,
	private val fileProvider: ProvideLibraryFiles,
	private val mediaItemServiceFileLookup: GetMediaItemsFromServiceFiles,
) : BrowseMediaItems {
	companion object {
		private fun toMediaItem(item: IItem): MediaBrowserCompat.MediaItem =
			MediaBrowserCompat.MediaItem(
				MediaDescriptionCompat
					.Builder()
					.setMediaId(
						(when (item) {
							is Item -> RemoteBrowserService.itemFileMediaIdPrefix
							is Playlist -> RemoteBrowserService.playlistFileMediaIdPrefix
							else -> ""
						}) + RemoteBrowserService.mediaIdDelimiter + item.key)
					.setTitle(item.value)
					.build(),
				MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
			)
	}

	override fun promiseItems(itemId: ItemId): Promise<Collection<MediaBrowserCompat.MediaItem>> =
		selectedLibraryIdProvider.promiseSelectedLibraryId().eventually { maybeId ->
			maybeId
				?.let { libraryId ->
					itemProvider
						.promiseItems(libraryId, itemId)
						.eventually { items ->
							if (items.any()) items.map(::toMediaItem).toPromise()
							else {
								fileProvider
									.promiseFiles(libraryId, itemId)
									.eventually<Collection<MediaBrowserCompat.MediaItem>> { files ->
										Promise.whenAll(files.map { f -> mediaItemServiceFileLookup.promiseMediaItem(libraryId, f).then { mi -> Pair(f, mi) } })
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
																		itemId.id,
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

	override fun promiseItems(playlistId: PlaylistId): Promise<Collection<MediaBrowserCompat.MediaItem>> =
		selectedLibraryIdProvider.promiseSelectedLibraryId().eventually { maybeId ->
			maybeId
				?.let { libraryId ->
					fileProvider
						.promiseFiles(libraryId, playlistId)
						.eventually<Collection<MediaBrowserCompat.MediaItem>> { files ->
							Promise.whenAll(files.map { f -> mediaItemServiceFileLookup.promiseMediaItem(libraryId, f).then { mi -> Pair(f, mi) } })
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
															RemoteBrowserService.playlistFileMediaIdPrefix,
															playlistId.id,
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
				.keepPromise(emptyList())
		}


	override fun promiseLibraryItems(): Promise<List<MediaBrowserCompat.MediaItem>> =
		selectedLibraryIdProvider.promiseSelectedLibraryId().eventually { maybeId ->
			maybeId
				?.let { libraryId -> itemProvider.promiseItems(libraryId).then { v -> v.map(::toMediaItem) } }
				.keepPromise(emptyList())
		}

	override fun promiseItems(query: String): Promise<Collection<MediaBrowserCompat.MediaItem>> {
		return selectedLibraryIdProvider.promiseSelectedLibraryId()
			.eventually { maybeId ->
				maybeId
					?.let { libraryId ->
						fileProvider
							.promiseAudioFiles(libraryId, query)
							.cancelBackEventually { files ->
								Promise.whenAll(files.map { mediaItemServiceFileLookup.promiseMediaItem(libraryId, it) })
							}
					}
					.keepPromise(emptyList())
			}
	}
}
