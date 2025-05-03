package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaybackEngine.AndPlaybackPlaysThroughCompletion

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val libraryId = 455

class WhenObservingPlayback {
	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library(id = libraryId)
		val libraryProvider = FakeLibraryRepository(library)
		val preparedPlaybackQueueResourceManagement = PreparedPlaybackQueueResourceManagement(
			fakePlaybackPreparerProvider,
			mockk {
				every { maxQueueSize } returns 1
			}
		)
		val nowPlayingRepository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
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

	private var isPlaying = false
	private var firstPlayingFile: PositionedPlayingFile? = null
	private var isCompleted = false
	private var playbackStarted = false

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, playbackEngine) = mut

		val countDownLatch = CountDownLatch(6)
		playbackEngine
			.setOnPlaybackStarted { playbackStarted = true }
			.setOnPlayingFileChanged { _, p ->
				if (firstPlayingFile == null) firstPlayingFile = p
				countDownLatch.countDown()
			}
			.setOnPlaybackCompleted {
				isCompleted = true
				countDownLatch.countDown()
			}
			.startPlaylist(
				LibraryId(libraryId),
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				),
				0
			)
			.toExpiringFuture()
			.get()
		var playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
		for (i in 0..3) {
			val newPlayingPlaybackHandler =
				fakePlaybackPreparerProvider.deferredResolution.resolve()
			playingPlaybackHandler.resolve()
			playingPlaybackHandler = newPlayingPlaybackHandler
		}
		playingPlaybackHandler.resolve()
		countDownLatch.await(1, TimeUnit.SECONDS)
		isPlaying = playbackEngine.isPlaying
	}

	@Test
	fun `then playback is started`() {
		assertThat(playbackStarted).isTrue
	}

	@Test
	fun `then the first playing file is the first service file`() {
		assertThat(firstPlayingFile!!.serviceFile).isEqualTo(ServiceFile("1"))
	}

	@Test
	fun `then the playlist is not playing`() {
		assertThat(isPlaying).isFalse
	}

	@Test
	fun `then the playback is completed`() {
		assertThat(isCompleted).isTrue
	}
}
