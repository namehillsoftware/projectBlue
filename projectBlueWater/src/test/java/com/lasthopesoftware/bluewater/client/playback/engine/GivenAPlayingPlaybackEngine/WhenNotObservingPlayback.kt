package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test

class WhenNotObservingPlayback {

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()

		val library = Library(id = 1, nowPlayingId = 5)

		val libraryProvider = FakeLibraryRepository(library)
		val promisedLibraryStorageUpdate = object : Promise<Unit>() {
			val storage = mockk<ILibraryStorage> {
				every { updateNowPlaying(any(), any(), any(), any(), any()) } answers {
					libraryProvider.updateNowPlaying(arg(0), arg(1), arg(2), arg(3), arg(4)).then { _ ->
						if (libraryProvider.libraries[firstArg<LibraryId>().id]?.nowPlayingId == 1)
							resolve(Unit)
					}
				}
			}
		}

		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
			listOf(CompletingFileQueueProvider()),
			NowPlayingRepository(
				FakeSelectedLibraryProvider(),
				libraryProvider,
				promisedLibraryStorageUpdate.storage,
				FakeNowPlayingState(),
			),
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)
		playbackEngine
			.startPlaylist(
				library.libraryId,
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				), 0, Duration.ZERO
			)
			.toExpiringFuture()
			.get()
		val resolvablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
		fakePlaybackPreparerProvider.deferredResolution.resolve()
		resolvablePlaybackHandler.resolve()
		promisedLibraryStorageUpdate.toExpiringFuture().get()
		Pair(libraryProvider, playbackEngine)
	}

	@Test
	fun thenTheSavedTrackPositionIsOne() {
		assertThat(mut.first.libraries[1]!!.nowPlayingId).isEqualTo(1)
	}

	@Test
	fun thenTheManagerIsPlaying() {
		assertThat(mut.second.isPlaying).isTrue
	}
}
