package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndTheTrackIsSkipped

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When changing the track back to the skipped track` {
	companion object {
		private const val libraryId = 434
	}

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
		val library = Library(id = libraryId)
		val libraryProvider = FakeLibraryRepository(library)
		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
		val repository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			libraryProvider,
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			repository,
			listOf(CompletingFileQueueProvider()),
		)
		val playbackEngine =
			PlaybackEngine(
				preparedPlaybackQueueResourceManagement,
				listOf(CompletingFileQueueProvider()),
				repository,
				playbackBootstrapper,
				playbackBootstrapper,
			)
		Pair(fakePlaybackPreparerProvider, playbackEngine)
	}

	private var nextSwitchedFile: PositionedFile? = null

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, playbackEngine) = mut

		val promisedPlaybackStarted = Promise {
			playbackEngine
				.setOnPlayingFileChanged { _, p ->
					it.sendResolution(p)
				}
		}

		val promisedStart = playbackEngine
			.startPlaylist(
				LibraryId(libraryId),
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				),
				1
			)

		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()
		promisedStart.toExpiringFuture().get()
		promisedPlaybackStarted.toExpiringFuture().get()

		val promisedNextTrackStarted = Promise {
			playbackEngine
				.setOnPlayingFileChanged { _, p ->
					it.sendResolution(p)
				}
		}

		playingPlaybackHandler?.resolve()

		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("3")]?.resolve()
		promisedNextTrackStarted.toExpiringFuture().get()

		val promisedSkipBack = playbackEngine.skipToPrevious()
		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()

		nextSwitchedFile = promisedSkipBack.toExpiringFuture().get()?.second
	}

	@Test
	fun `then the engine is playing`() {
		assertThat(mut.second.isPlaying).isTrue
	}

	@Test
	fun `then the next file change is the switched to the correct track position`() {
		assertThat(nextSwitchedFile?.playlistPosition).isEqualTo(1)
	}
}
