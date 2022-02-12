package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaylistEngine.AndPlaybackPlaysThroughCompletion

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WhenObservingPlayback {
	@Test
	fun thenPlaybackIsStarted() {
		assertThat(playbackStarted).isTrue
	}

	@Test
	fun thenTheFirstPlayingFileIsTheFirstServiceFile() {
		assertThat(firstPlayingFile!!.serviceFile).isEqualTo(ServiceFile(1))
	}

	@Test
	fun thenThePlaylistIsNotPlaying() {
		assertThat(isPlaying).isFalse
	}

	@Test
	fun thenThePlaybackIsCompleted() {
		assertThat(isCompleted).isTrue
	}

	companion object {
		private var isPlaying = false
		private var firstPlayingFile: PositionedPlayingFile? = null
		private var isCompleted = false
		private var playbackStarted = false

		@BeforeClass
		@JvmStatic
		fun context() {
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
                        libraryStorage
                    ),
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				)

			val countDownLatch = CountDownLatch(6)
			playbackEngine
				.setOnPlaybackStarted { playbackStarted = true }
				.setOnPlayingFileChanged { p: PositionedPlayingFile? ->
					if (firstPlayingFile == null) firstPlayingFile = p
					countDownLatch.countDown()
				}
				.setOnPlaybackCompleted {
					isCompleted = true
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
	}
}
