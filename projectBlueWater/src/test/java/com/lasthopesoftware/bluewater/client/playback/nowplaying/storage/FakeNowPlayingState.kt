package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import java.util.concurrent.ConcurrentHashMap

class FakeNowPlayingState : HoldNowPlayingState {
	private val nowPlayingCache = ConcurrentHashMap<LibraryId, NowPlaying>()

	override fun set(libraryId: LibraryId, nowPlaying: NowPlaying) {
		nowPlayingCache[libraryId] = nowPlaying
	}

	override fun get(libraryId: LibraryId): NowPlaying? = nowPlayingCache[libraryId]
}
