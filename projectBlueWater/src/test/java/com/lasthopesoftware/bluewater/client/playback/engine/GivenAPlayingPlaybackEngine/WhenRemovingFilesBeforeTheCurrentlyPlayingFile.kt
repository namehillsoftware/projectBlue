package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine.Companion.createEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenRemovingFilesBeforeTheCurrentlyPlayingFile {

	companion object {
		private val fileQueueProvider = spyk(CompletingFileQueueProvider())
		private val library = Library()
		private var nowPlaying: NowPlaying? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			library.setId(1)
			library.setSavedTracksString(
				FileStringListUtilities.promiseSerializedFileStringList(
					listOf(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(3),
						ServiceFile(4),
						ServiceFile(5)
					)
				)
					.toFuture().get()
			)
			library.setNowPlayingId(2)

			val libraryProvider = mockk<ISpecificLibraryProvider>()
			every { libraryProvider.library } returns Promise(library)

			val libraryStorage = PassThroughLibraryStorage()

			val repository = NowPlayingRepository(libraryProvider, libraryStorage)
			val playbackEngine =
				createEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 }, listOf(fileQueueProvider),
					NowPlayingRepository(libraryProvider, libraryStorage),
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				).toFuture().get()

			playbackEngine!!.resume().toFuture()[1, TimeUnit.SECONDS]
			val resolvablePlaybackHandler =
				fakePlaybackPreparerProvider.deferredResolution.resolve()
			resolvablePlaybackHandler.setCurrentPosition(35)
			playbackEngine.removeFileAtPosition(0).toFuture()[1, TimeUnit.SECONDS]
			resolvablePlaybackHandler.setCurrentPosition(92)
			playbackEngine.pause().toFuture().get()
			nowPlaying = repository.nowPlaying.toFuture().get()
		}
	}

	@Test
	fun thenTheCurrentlyPlayingFileShifts() {
		assertThat(library.nowPlayingId).isEqualTo(1)
	}

	@Test
	fun thenTheFileQueueIsShifted() {
		verify(exactly = 2) { fileQueueProvider.provideQueue(any(), match { i -> i == 2 }) }
	}

	@Test
	fun thenTheCurrentlyPlayingFileStillTracksFileProgress() {
		assertThat(nowPlaying!!.filePosition).isEqualTo(92)
	}
}
