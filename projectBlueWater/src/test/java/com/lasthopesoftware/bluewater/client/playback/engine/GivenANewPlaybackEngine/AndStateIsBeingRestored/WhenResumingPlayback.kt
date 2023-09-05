package com.lasthopesoftware.bluewater.client.playback.engine.GivenANewPlaybackEngine.AndStateIsBeingRestored

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 858

class WhenResumingPlayback {
	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library(
			id = libraryId,
			nowPlayingProgress = 543,
			nowPlayingId = 586,
		)

		val deferredNowPlaying = DeferredPromise<NowPlaying?>(
			NowPlaying(
				libraryId = LibraryId(libraryId),
				playlist = listOf(ServiceFile(312), ServiceFile(982), ServiceFile(83)),
				playlistPosition = 0,
				filePosition = 556,
				isRepeating = false
			)
		)

		val engine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
			listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
			mockk {
				every { promiseNowPlaying(library.libraryId) } returns deferredNowPlaying
			},
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Pair(deferredNowPlaying, engine)
	}

	private var restoredState: Pair<LibraryId, PositionedProgressedFile?>? = null

	@BeforeAll
	fun act() {
		val (deferredNowPlaying, engine) = mut

		val futureRestoredState = engine.restoreFromSavedState(LibraryId(libraryId)).toExpiringFuture()

		val futureResume = engine.resume().toExpiringFuture()

		deferredNowPlaying.resolve()

		restoredState = futureRestoredState.get()
		futureResume.get()
	}

	@Test
	fun `then the restored state is correct`() {
		assertThat(restoredState).isEqualTo(Pair(LibraryId(libraryId), PositionedProgressedFile(0, ServiceFile(312), Duration.millis(556))))
	}
}
