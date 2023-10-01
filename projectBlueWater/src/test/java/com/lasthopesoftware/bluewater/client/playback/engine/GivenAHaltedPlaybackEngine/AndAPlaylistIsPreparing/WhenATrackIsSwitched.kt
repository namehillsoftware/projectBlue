package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaybackEngine.AndAPlaylistIsPreparing

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test

private const val libraryId = 428

class WhenATrackIsSwitched {

	private val nextSwitchedFile by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library(id = libraryId)

		val libraryProvider = FakeLibraryRepository(library)

		val playbackEngine =
			PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(
					fakePlaybackPreparerProvider,
					mockk {
						every { maxQueueSize } returns 1
					}
				),
				listOf(CompletingFileQueueProvider()),
				NowPlayingRepository(
					mockk(),
					libraryProvider,
					libraryProvider,
					FakeNowPlayingState(),
				),
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
		playbackEngine
			.startPlaylist(
				LibraryId(libraryId),
				mutableListOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(3),
					ServiceFile(4),
					ServiceFile(5)
				), 0, Duration.ZERO
			)
			.toExpiringFuture()
			.get()
		val futurePositionedFile = playbackEngine.changePosition(3, Duration.ZERO).toExpiringFuture()
		fakePlaybackPreparerProvider.deferredResolution.resolve()
		futurePositionedFile.get()
	}

	@Test
	fun `then the next file change is the switched to the correct track position`() {
		assertThat(nextSwitchedFile?.second?.playlistPosition).isEqualTo(3)
	}
}
