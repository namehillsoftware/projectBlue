package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.access.ManageLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
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
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
			listOf(
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("3"),
				ServiceFile("4"),
				ServiceFile("5")
			)
		)

		val library = Library(id = 1, nowPlayingId = 5)

		val libraryProvider = FakeLibraryRepository(library)
		val promisedLibraryStorageUpdate = object : Promise<Unit>() {
			val storage = mockk<ManageLibraries> {
				every { updateNowPlaying(any()) } answers {
					val values = firstArg<LibraryNowPlayingValues>()
					libraryProvider.updateNowPlaying(values).then { _ ->
						if (libraryProvider.libraries[values.id]?.nowPlayingId == 1)
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
				promisedLibraryStorageUpdate.storage
			),
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)
		playbackEngine
			.startPlaylist(
				library.libraryId,
				fakePlaybackPreparerProvider.deferredResolutions.keys.toList(),
				0,
				Duration.ZERO
			)
			.toExpiringFuture()
			.get()
		val resolvablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()
		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()
		resolvablePlaybackHandler?.resolve()
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
