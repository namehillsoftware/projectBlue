package com.lasthopesoftware.bluewater.client.playback.nowplaying

import androidx.lifecycle.AtomicReference
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.tryUpdate
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class FakeNowPlayingRepository(
	private var selectedLibraryId: LibraryId? = null,
	vararg initialRepository: NowPlaying
) : ManageNowPlayingState {
	constructor(vararg initialRepository: NowPlaying)
		: this(initialRepository.firstOrNull()?.libraryId, *initialRepository)

	private val sync = Any()
	private val repository = initialRepository.associateBy { it.libraryId }.toMutableMap()

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> = synchronized(sync) {
		repository[nowPlaying.libraryId] = nowPlaying
		return nowPlaying.toPromise()
	}

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> = synchronized(sync) {
		selectedLibraryId?.let(repository::get)?.toPromise().keepPromise()
	}

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> = synchronized(sync) {
		repository[libraryId]?.toPromise().keepPromise()
	}
}

interface Gate {
	fun open()
	fun close()
}

class LockingNowPlayingRepository(private val inner: ManageNowPlayingState) : ManageNowPlayingState, Gate {

	private val latch = AtomicReference(DeferredPromise(Unit))

	override fun open() {
		latch.get().resolve()
	}

	override fun close() {
		latch.tryUpdate {
			it.resolve()
			DeferredPromise(Unit)
		}
	}

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> = latch.get().eventually {
		inner.updateNowPlaying(nowPlaying)
	}

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> = latch.get().eventually {
		inner.promiseActiveNowPlaying()
	}

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> = latch.get().eventually {
		inner.promiseNowPlaying(libraryId)
	}
}

class AlwaysOpenNowPlayingRepository<T>(inner: T) : ManageNowPlayingState by inner, Gate where T : ManageNowPlayingState, T : Gate {
	init { inner.open() }

	override fun open() {}

	override fun close() {}
}
