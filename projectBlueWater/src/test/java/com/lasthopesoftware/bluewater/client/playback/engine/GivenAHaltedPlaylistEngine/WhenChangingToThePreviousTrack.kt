package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaylistEngine

import com.lasthopesoftware.EmptyUrl
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine.Companion.createEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenChangingToThePreviousTrack {

	companion object {
		private val libraryUnderTest = Library()
		private var nextSwitchedFile: PositionedFile? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			libraryUnderTest.setId(1)
			libraryUnderTest.setSavedTracksString(
				FileStringListUtilities.promiseSerializedFileStringList(
					listOf(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(3),
						ServiceFile(4),
						ServiceFile(5)
					)
				).toFuture().get()
			)
			libraryUnderTest.setNowPlayingId(4)

			val libraryProvider = object : ISpecificLibraryProvider {
				override val library: Promise<Library?>
					get() = Promise(libraryUnderTest)
			}

			val libraryStorage: ILibraryStorage = PassThroughLibraryStorage()
			val filePropertiesContainerRepository = mockk<IFilePropertiesContainerRepository>()
			every {
				filePropertiesContainerRepository.getFilePropertiesContainer(UrlKeyHolder(EmptyUrl.url,	ServiceFile(4)))
			} returns FilePropertiesContainer(1, mapOf(Pair(KnownFileProperties.DURATION, "100")))

			val playbackEngine =
				createEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 },
					listOf(CompletingFileQueueProvider()),
					NowPlayingRepository(libraryProvider, libraryStorage),
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				).toFuture().get()

			nextSwitchedFile = playbackEngine?.skipToPrevious()?.toFuture()?.get(1, TimeUnit.SECONDS)
		}
	}

	@Test
	fun thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile?.playlistPosition).isEqualTo(3)
	}

	@Test
	fun thenTheSavedLibraryIsAtTheCorrectTrackPosition() {
		assertThat(libraryUnderTest.nowPlayingId).isEqualTo(3)
	}
}
