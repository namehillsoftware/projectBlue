package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.SearchFileParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.ProvideLibraryViews
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class MediaItemsBrowser
(
	private val nowPlayingRepository: INowPlayingRepository,
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val itemProvider: ProvideItems,
	private val fileProvider: ProvideFiles,
	private val libraryViews: ProvideLibraryViews,
	private val mediaItemServiceFileLookup: GetMediaItemsFromServiceFiles,
) : BrowseMediaItems {
	companion object {
		private fun toMediaItem(item: Item): MediaBrowserCompat.MediaItem =
			MediaBrowserCompat.MediaItem(
				MediaDescriptionCompat
					.Builder()
					.setMediaId(RemoteBrowserService.itemFileMediaIdPrefix + item.key)
					.setTitle(item.value)
					.build(),
				MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
			)

		private fun toPlayableMediaItem(item: Item): MediaBrowserCompat.MediaItem =
			MediaBrowserCompat.MediaItem(
				MediaDescriptionCompat
					.Builder()
					.setMediaId(RemoteBrowserService.itemFileMediaIdPrefix + item.key)
					.setTitle(item.value)
					.build(),
				MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
			)
	}

	override fun promiseNowPlayingItem(): Promise<MediaBrowserCompat.MediaItem?> =
		nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				if (np.playlist.isEmpty() || np.playlistPosition < 0) Promise.empty<MediaBrowserCompat.MediaItem?>()
				else mediaItemServiceFileLookup.promiseMediaItem(np.playlist[np.playlistPosition])
			}

	override fun promiseItems(item: Item): Promise<Collection<MediaBrowserCompat.MediaItem>> =
		selectedLibraryIdProvider.selectedLibraryId.eventually { maybeId ->
			maybeId
				?.let { libraryId ->
					itemProvider
						.promiseItems(libraryId, item.key)
						.eventually { items ->
							if (items.any()) items.map(::toMediaItem).toPromise()
							else {
								val parameters = FileListParameters.getInstance().getFileListParameters(item)
								fileProvider
									.promiseFiles(FileListParameters.Options.None, *parameters)
									.eventually { files -> Promise.whenAll(files.map(mediaItemServiceFileLookup::promiseMediaItem)) }
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
		return fileProvider
			.promiseFiles(FileListParameters.Options.None, *parameters)
			.eventually { files -> Promise.whenAll(files.map(mediaItemServiceFileLookup::promiseMediaItem)) }
	}
}
