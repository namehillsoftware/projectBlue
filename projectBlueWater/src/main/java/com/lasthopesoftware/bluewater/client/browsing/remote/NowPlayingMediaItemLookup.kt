package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingMediaItemLookup(
	private val nowPlayingRepository: INowPlayingRepository,
	private val mediaItemServiceFileLookup: GetMediaItemsFromServiceFiles,
) : GetNowPlayingMediaItem {
	override fun promiseNowPlayingItem(): Promise<MediaBrowserCompat.MediaItem?> =
		nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				if (np.playlist.isEmpty() || np.playlistPosition < 0) Promise.empty<MediaBrowserCompat.MediaItem?>()
				else mediaItemServiceFileLookup.promiseMediaItemWithImage(np.playlist[np.playlistPosition])
			}
}
