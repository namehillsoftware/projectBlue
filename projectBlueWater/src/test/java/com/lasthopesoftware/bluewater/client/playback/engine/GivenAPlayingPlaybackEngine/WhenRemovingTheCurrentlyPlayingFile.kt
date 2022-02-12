package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.EmptyUrl
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.TimeUnit

class WhenRemovingTheCurrentlyPlayingFile {

	companion object {
		private var initialState: PositionedProgressedFile? = null
		private val fileQueueProvider = Mockito.spy(CompletingFileQueueProvider())
		private val library = Library()
		private var positionedPlayingFile: PositionedPlayingFile? = null

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
							ServiceFile(5),
							ServiceFile(13),
							ServiceFile(27)
						)
					).toFuture().get()
			)
			library.setNowPlayingId(5)
			val libraryProvider = mockk<ISpecificLibraryProvider>()
			every { libraryProvider.library } returns Promise(library)

			val libraryStorage = PassThroughLibraryStorage()
			val filePropertiesContainerRepository = mockk<IFilePropertiesContainerRepository>()
			every {
				filePropertiesContainerRepository.getFilePropertiesContainer(UrlKeyHolder(EmptyUrl.url, ServiceFile(5)))
			} returns FilePropertiesContainer(1, mapOf(Pair(KnownFileProperties.DURATION, "100")))

			val playbackEngine =
				PlaybackEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 }, listOf(fileQueueProvider),
                    NowPlayingRepository(
                        libraryProvider,
                        libraryStorage
                    ),
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				)

			initialState = playbackEngine.restoreFromSavedState().toFuture().get()
			playbackEngine.resume().toFuture()[1, TimeUnit.SECONDS]
			fakePlaybackPreparerProvider.deferredResolution.resolve()
			playbackEngine.setOnPlayingFileChanged { c -> positionedPlayingFile = c	}
			val futurePlaying = playbackEngine.removeFileAtPosition(5).toFuture()
			fakePlaybackPreparerProvider.deferredResolution.resolve()
			futurePlaying[1, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenTheCurrentlyPlayingFilePositionIsTheSame() {
		assertThat(library.nowPlayingId).isEqualTo(5)
	}

	@Test
	fun thenTheFileQueueIsShiftedToTheNextFile() {
		assertThat(positionedPlayingFile!!.serviceFile).isEqualTo(ServiceFile(27))
	}

	@Test
	fun `then the initial service file is correct`() {
		assertThat(initialState?.serviceFile).isEqualTo(ServiceFile(13))
	}
}
