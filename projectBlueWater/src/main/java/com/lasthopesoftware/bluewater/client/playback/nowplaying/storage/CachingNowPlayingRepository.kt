package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.PermanentPromiseFunctionCache
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class CachingNowPlayingRepository(
	private val selectedLibraryId: ProvideSelectedLibraryId,
	private val inner: ManageNowPlayingState,
	private val cache: CachePromiseFunctions<LibraryId, NowPlaying?> = PermanentPromiseFunctionCache(),
) : ManageNowPlayingState {
	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
		cache.overrideCachedValue(nowPlaying.libraryId, nowPlaying)
		inner.updateNowPlaying(nowPlaying)
		return nowPlaying.toPromise()
	}

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> =
		selectedLibraryId
			.promiseSelectedLibraryId()
			.cancelBackEventually { id -> id?.let(::promiseNowPlaying).keepPromise() }

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> = cache.getOrAdd(libraryId) {
		inner.promiseNowPlaying(libraryId)
	}
}
