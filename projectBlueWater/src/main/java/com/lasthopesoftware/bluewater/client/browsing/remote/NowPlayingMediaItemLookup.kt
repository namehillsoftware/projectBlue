package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class NowPlayingMediaItemLookup(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val nowPlayingRepository: MaintainNowPlayingState,
	private val mediaItemServiceFileLookup: GetMediaItemsFromServiceFiles,
) : GetNowPlayingMediaItem {
	override fun promiseNowPlayingItem(): Promise<MediaBrowserCompat.MediaItem?> =
		selectedLibraryIdProvider
			.promiseSelectedLibraryId()
			.eventually {
				it?.let(nowPlayingRepository::promiseNowPlaying).keepPromise()
			}
			.eventually { np ->
				if (np == null || np.playlist.isEmpty() || np.playlistPosition < 0) Promise.empty<MediaBrowserCompat.MediaItem?>()
				else mediaItemServiceFileLookup.promiseMediaItemWithImage(np.playlist[np.playlistPosition])
			}
}
