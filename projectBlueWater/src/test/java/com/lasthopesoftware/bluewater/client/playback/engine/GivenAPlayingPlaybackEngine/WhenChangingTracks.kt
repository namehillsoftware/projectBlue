package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine.Companion.createEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.concurrent.CountDownLatch

class WhenChangingTracks {
	@Test
	fun thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile!!.playlistPosition).isEqualTo(3)
	}

	@Test
	fun thenTheLatestObservedFileIsAtTheCorrectTrackPosition() {
		assertThat(latestFile!!.playlistPosition).isEqualTo(3)
	}

	@Test
	fun thenTheFirstStartedFileIsCorrect() {
		assertThat(startedFiles[0].asPositionedFile())
			.isEqualTo(PositionedFile(0, ServiceFile(1)))
	}

	@Test
	fun thenTheChangedStartedFileIsCorrect() {
		assertThat(startedFiles[1].asPositionedFile())
			.isEqualTo(PositionedFile(3, ServiceFile(4)))
	}

	@Test
	fun thenThePlaylistIsStartedTwice() {
		assertThat(startedFiles).hasSize(2)
	}

	companion object {
		private var nextSwitchedFile: PositionedFile? = null
		private var latestFile: PositionedPlayingFile? = null
		private val startedFiles: MutableList<PositionedPlayingFile> = ArrayList()

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			val library = Library()
			library.setId(1)
			val libraryProvider = PassThroughSpecificLibraryProvider(library)
			val libraryStorage = PassThroughLibraryStorage()
			val playbackEngine = FuturePromise(
				createEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 }, listOf(CompletingFileQueueProvider()),
					NowPlayingRepository(libraryProvider, libraryStorage),
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				)
			).get()
			val countDownLatch = CountDownLatch(2)
			playbackEngine
				?.setOnPlayingFileChanged { p ->
					startedFiles.add(p)
					latestFile = p
					countDownLatch.countDown()
				}
				?.startPlaylist(
					listOf(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(3),
						ServiceFile(4),
						ServiceFile(5)
					), 0, Duration.ZERO
				)
			val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
			playbackEngine!!.changePosition(3, Duration.ZERO).then<Any?> { p: PositionedFile? ->
				nextSwitchedFile = p
				countDownLatch.countDown()
				null
			}
			fakePlaybackPreparerProvider.deferredResolution.resolve()
			playingPlaybackHandler.resolve()
			countDownLatch.await()
		}
	}
}
