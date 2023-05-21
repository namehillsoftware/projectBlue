package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingMediaItemLookup(
	private val nowPlayingRepository: MaintainNowPlayingState,
	private val mediaItemServiceFileLookup: GetMediaItemsFromServiceFiles,
) : GetNowPlayingMediaItem {
	override fun promiseNowPlayingItem(libraryId: LibraryId): Promise<MediaBrowserCompat.MediaItem?> =
		nowPlayingRepository
			.promiseNowPlaying(libraryId)
			.eventually { np ->
				if (np == null || np.playlist.isEmpty() || np.playlistPosition < 0) Promise.empty<MediaBrowserCompat.MediaItem?>()
				else mediaItemServiceFileLookup.promiseMediaItemWithImage(np.playlist[np.playlistPosition])
			}
}
