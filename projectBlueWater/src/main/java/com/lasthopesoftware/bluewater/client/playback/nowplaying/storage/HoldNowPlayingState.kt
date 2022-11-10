package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface HoldNowPlayingState {
	operator fun set(libraryId: LibraryId, nowPlaying: NowPlaying)

	operator fun get(libraryId: LibraryId): NowPlaying?
}
