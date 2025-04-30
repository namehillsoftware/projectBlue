package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaybackEngine.AndAPlaylistIsPreparing

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenATrackIsSwitched {

	companion object {
		private const val libraryId = 428

		private val playlist = listOf(
			ServiceFile("1"),
			ServiceFile("2"),
			ServiceFile("3"),
			ServiceFile("4"),
			ServiceFile("5")
		)
	}

	private val mutt by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(playlist)
		val library = Library(id = libraryId)

		val libraryProvider = FakeLibraryRepository(library)

		val preparedPlaybackQueueResourceManagement = PreparedPlaybackQueueResourceManagement(
			fakePlaybackPreparerProvider,
			mockk {
				every { maxQueueSize } returns 1
			}
		)
		val nowPlayingRepository = NowPlayingRepository(
			mockk(),
			libraryProvider,
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			nowPlayingRepository,
			listOf(CompletingFileQueueProvider()),
		)
		val playbackEngine =
			PlaybackEngine(
				preparedPlaybackQueueResourceManagement,
				listOf(CompletingFileQueueProvider()),
				nowPlayingRepository,
				playbackBootstrapper,
				playbackBootstrapper,
			)

		Pair(fakePlaybackPreparerProvider, playbackEngine)
	}

	private var nextSwitchedFile: Pair<LibraryId, PositionedFile>? = null
	private var error: Throwable? = null

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, playbackEngine) = mutt

		playbackEngine.setOnPlaylistError {
			error = it
		}

		playbackEngine
			.startPlaylist(LibraryId(libraryId), playlist, 0)
			.toExpiringFuture()
			.get()
		val futurePositionedFile = playbackEngine.changePosition(3, Duration.ZERO).toExpiringFuture()
		fakePlaybackPreparerProvider.deferredResolutions[playlist[3]]?.resolve()
		nextSwitchedFile = futurePositionedFile.get()
	}

	@Test
	fun `then the error is null`() {
		assertThat(error).isNull()
	}

	@Test
	fun `then the next file change is the switched to the correct track position`() {
		assertThat(nextSwitchedFile?.second?.playlistPosition).isEqualTo(3)
	}
}
