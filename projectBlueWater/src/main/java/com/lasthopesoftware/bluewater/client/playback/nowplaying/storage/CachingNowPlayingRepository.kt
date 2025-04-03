package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class CachingNowPlayingRepository(
	private val inner: ManageNowPlayingState,
	private val cache: HoldNowPlayingState = InMemoryNowPlayingState,
) : ManageNowPlayingState {
	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
		cache[nowPlaying.libraryId] = nowPlaying
		inner.updateNowPlaying(nowPlaying)
		return nowPlaying.toPromise()
	}

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> =
		inner
			.promiseActiveNowPlaying()
			.cancelBackThen { nowPlaying, _ -> nowPlaying?.also { cache[it.libraryId] = it } }

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> =
		inner
			.promiseNowPlaying(libraryId)
			.cancelBackThen { nowPlaying, _ -> nowPlaying?.also { cache[it.libraryId] = it } }
}
