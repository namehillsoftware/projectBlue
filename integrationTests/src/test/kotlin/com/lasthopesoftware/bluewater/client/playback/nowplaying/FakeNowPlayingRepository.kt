package com.lasthopesoftware.bluewater.client.playback.nowplaying

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class FakeNowPlayingRepository(var selectedLibraryId: LibraryId? = null, vararg initialRepository: NowPlaying) : ManageNowPlayingState {
	constructor(vararg initialRepository: NowPlaying)
		: this(initialRepository.firstOrNull()?.libraryId, *initialRepository)

	private val repository = mutableMapOf(*initialRepository.map { Pair(it.libraryId, it) }.toTypedArray())

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
		repository[nowPlaying.libraryId] = nowPlaying
		return nowPlaying.toPromise()
	}

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> =
		selectedLibraryId?.let(repository::get)?.toPromise().keepPromise()

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> =
		repository[libraryId]?.toPromise().keepPromise()
}
