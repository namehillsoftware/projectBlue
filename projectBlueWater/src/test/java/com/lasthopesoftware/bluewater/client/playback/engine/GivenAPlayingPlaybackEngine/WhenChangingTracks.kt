package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class WhenChangingTracks {

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library()
		library.setId(1)
		val libraryProvider = PassThroughSpecificLibraryProvider(library)
		val libraryStorage = PassThroughLibraryStorage()
		val playbackEngine =
			PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(
					fakePlaybackPreparerProvider
				) { 1 }, listOf(CompletingFileQueueProvider()),
				NowPlayingRepository(
					libraryProvider,
					libraryStorage,
					FakeNowPlayingState(),
				),
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
		Pair(fakePlaybackPreparerProvider, playbackEngine)
	}

	private var nextSwitchedFile: PositionedFile? = null
	private var latestFile: PositionedPlayingFile? = null
	private val startedFiles: MutableList<PositionedPlayingFile> = ArrayList()

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, playbackEngine) = mut

		val countDownLatch = CountDownLatch(1)
		playbackEngine
			.setOnPlayingFileChanged { p ->
				startedFiles.add(p)
				latestFile = p
				countDownLatch.countDown()
			}
			.startPlaylist(
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(3),
					ServiceFile(4),
					ServiceFile(5)
				), 0, Duration.ZERO
			)

		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
		val futurePositionChange = playbackEngine.changePosition(3, Duration.ZERO).toExpiringFuture()
		fakePlaybackPreparerProvider.deferredResolution.resolve()
		playingPlaybackHandler.resolve()

		countDownLatch.await()
		nextSwitchedFile = futurePositionChange.get()
	}

	@Test
	fun `then the next file change is the switched to the correct track position`() {
		assertThat(nextSwitchedFile!!.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the latest observed file is at the correct track position`() {
		assertThat(latestFile!!.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the first started file is correct`() {
		assertThat(startedFiles[0].asPositionedFile())
			.isEqualTo(PositionedFile(0, ServiceFile(1)))
	}

	@Test
	fun `then the changed started file is correct`() {
		assertThat(startedFiles[1].asPositionedFile())
			.isEqualTo(PositionedFile(3, ServiceFile(4)))
	}

	@Test
	fun `then the playlist is started twice`() {
		assertThat(startedFiles).hasSize(2)
	}
}
