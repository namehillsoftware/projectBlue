package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenClearingThePlaylist {

	private val mut by lazy {
		val storedLibrary = Library(
			id = 1,
			savedTracksString = FileStringListUtilities.promiseSerializedFileStringList(
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(3),
					ServiceFile(4),
					ServiceFile(5)
				)
			).toExpiringFuture().get(),
			nowPlayingId = 3,
		)

		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val libraryProvider = FakeLibraryRepository(storedLibrary)

		PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				mockk {
					every { maxQueueSize } returns 1
				}
			),
			listOf(CompletingFileQueueProvider()),
			NowPlayingRepository(
				FakeSelectedLibraryProvider(),
				libraryProvider,
				libraryProvider,
				FakeNowPlayingState(),
			),
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)
			.setOnPlaybackCompleted { isPlaybackCompletionSignaled = true }
	}

	private var isPlaybackCompletionSignaled = false
	private var updatedNowPlaying: NowPlaying? = null

	@BeforeAll
	fun act() {
		mut
			.startPlaylist(
				LibraryId(1),
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(3),
					ServiceFile(4),
					ServiceFile(5)
				),
				0,
				Duration.standardMinutes(1)
			)
			.toExpiringFuture()
			.get()
		updatedNowPlaying = mut.clearPlaylist().toExpiringFuture()[1, TimeUnit.SECONDS]
	}

	@Test
	fun `then playback completion is signaled`() {
		assertThat(isPlaybackCompletionSignaled).isTrue
	}

	@Test
	fun `then the playlist is updated`() {
		assertThat(updatedNowPlaying?.playlist).isEmpty()
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(updatedNowPlaying?.playlistPosition).isEqualTo(0)
	}

	@Test
	fun `then the file position is correct`() {
		assertThat(updatedNowPlaying?.filePosition).isEqualTo(0)
	}

	@Test
	fun `then it is not playing`() {
		assertThat(mut.isPlaying).isFalse
	}
}
