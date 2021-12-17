package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.namehillsoftware.handoff.promises.Promise

class MediaItemsBrowser
(
	private val nowPlayingRepository: INowPlayingRepository,
	private val mediaItemServiceFileLookup: GetMediaItemsFromServiceFiles,
) : BrowseMediaItems {
	override fun promiseNowPlayingItem(): Promise<MediaBrowserCompat.MediaItem> {
		return nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				if (np.playlist.isEmpty() || np.playlistPosition < 0) Promise.empty()
				else mediaItemServiceFileLookup.promiseMediaItem(np.playlist[np.playlistPosition])
			}
	}

	override fun promiseItems(item: Item): Promise<List<MediaBrowserCompat.MediaItem>> {
		TODO("Not yet implemented")
	}

	override fun promiseLibraryItems(): Promise<List<MediaBrowserCompat.MediaItem>> {
		TODO("Not yet implemented")
	}

	override fun promiseSearchedItem(query: String): Promise<List<MediaBrowserCompat.MediaItem>> {
		TODO("Not yet implemented")
	}
}
