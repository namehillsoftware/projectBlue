package com.lasthopesoftware.bluewater.client.playback.engine.GivenANewPlaybackEngine

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
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenRestoringEngineStateAndResumingPlayback {
	companion object {
		private var positionedPlayingFile: PositionedPlayingFile? = null
		private val restoredState by lazy {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			val library = Library()
			library.setId(1)
			library.setSavedTracksString(
				FileStringListUtilities.promiseSerializedFileStringList(
					listOf(
						ServiceFile(32),
						ServiceFile(329),
						ServiceFile(580),
						ServiceFile(915),
						ServiceFile(899)
					)
				).toFuture().get()
			)
			library.setNowPlayingProgress(893)
			library.setNowPlayingId(3)
			val libraryProvider = object : ISpecificLibraryProvider {
				override val library: Promise<Library?>
					get() = library.toPromise()
			}

			val libraryStorage = PassThroughLibraryStorage()

			val filePropertiesContainerRepository = mockk<IFilePropertiesContainerRepository>()
			every {
				filePropertiesContainerRepository.getFilePropertiesContainer(
					UrlKeyHolder(
						EmptyUrl.url,
						ServiceFile(4)
					)
				)
			} returns FilePropertiesContainer(1, mapOf(Pair(KnownFileProperties.DURATION, "100")))

			val repository = NowPlayingRepository(libraryProvider, libraryStorage)
			val playbackEngine = PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider) { 1 },
				listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
				repository,
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
			val restoredState = playbackEngine.restoreFromSavedState().toFuture().get()

			playbackEngine.setOnPlayingFileChanged { c -> positionedPlayingFile = c	}
			val promisedResumption = playbackEngine.resume()
			fakePlaybackPreparerProvider.deferredResolution.resolve()
			promisedResumption.toFuture().get()

			restoredState
		}
	}

	@Test
	fun `then the playlist position is correct`() {
		assertThat(restoredState?.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the service file is correct`() {
		assertThat(restoredState?.serviceFile).isEqualTo(ServiceFile(915))
	}

	@Test
	fun `then the file progress is correct`() {
		assertThat(restoredState?.progress?.toFuture()?.get()?.millis).isEqualTo(893)
	}

	@Test
	fun `then the playing file playlist position is correct`() {
		assertThat(positionedPlayingFile?.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the playing file progress is correct`() {
		assertThat(positionedPlayingFile?.playingFile?.progress?.toFuture()?.get()?.millis).isEqualTo(893)
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(positionedPlayingFile?.serviceFile).isEqualTo(ServiceFile(915))
	}
}