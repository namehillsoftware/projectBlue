package com.lasthopesoftware.bluewater.client.playback.nowplaying

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.promises.Gate
import com.lasthopesoftware.promises.PromiseLatch
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.LinkedList

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

class LockingNowPlayingRepository(private val inner: ManageNowPlayingState) : ManageNowPlayingState, Gate, AutoCloseable {

	private val latch = PromiseLatch()

	override fun open(): Gate {
		latch.open()
		return this
	}

	override fun reset(): Promise<Boolean> = latch.reset()

	override fun close() {
		latch.reset()
	}

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> = latch.wait().eventually {
		inner.updateNowPlaying(nowPlaying)
	}

	override fun promiseActiveNowPlaying(): Promise<NowPlaying?> = latch.wait().eventually {
		inner.promiseActiveNowPlaying()
	}

	override fun promiseNowPlaying(libraryId: LibraryId): Promise<NowPlaying?> = latch.wait().eventually {
		inner.promiseNowPlaying(libraryId)
	}
}

class AlwaysOpenNowPlayingRepository<T>(inner: T) : ManageNowPlayingState by inner, AutoCloseable,
	Gate where T : ManageNowPlayingState, T : Gate {
	init { inner.open() }

	override fun open() = this
	override fun reset(): Promise<Boolean> = false.toPromise()

	override fun close() {}
}

class RecordingNowPlayingRepository(private val inner: ManageNowPlayingState) : ManageNowPlayingState by inner {
	val states = LinkedList<NowPlaying>()

	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
		states.push(nowPlaying)
		return inner.updateNowPlaying(nowPlaying)
	}
}

class LoggingNowPlayingRepository(private val inner: ManageNowPlayingState) : ManageNowPlayingState by inner {
	override fun updateNowPlaying(nowPlaying: NowPlaying): Promise<NowPlaying> {
		println("updateNowPlaying($nowPlaying)")
		return inner
			.updateNowPlaying(nowPlaying)
			.then { np ->
				println("return $np")
				np
			}
	}
}
