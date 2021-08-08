package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaylistEngine

import com.lasthopesoftware.EmptyUrl
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine.Companion.createEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying
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

class WhenSettingEngineToComplete {

	companion object {
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
				).toFuture().get()
			)
			library.setNowPlayingId(0)
			library.setRepeating(true)
			val libraryProvider = mockk<ISpecificLibraryProvider>()
			every { libraryProvider.library } returns Promise(library)

			val libraryStorage = PassThroughLibraryStorage()
			val filePropertiesContainerRepository = mockk<IFilePropertiesContainerRepository>()
			every {
				filePropertiesContainerRepository.getFilePropertiesContainer(UrlKeyHolder(EmptyUrl.url, ServiceFile(4)))
			} returns FilePropertiesContainer(1, mapOf(Pair(KnownFileProperties.DURATION, "100")))

			val repository = NowPlayingRepository(libraryProvider, libraryStorage)
			val playbackEngine =
				createEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 },
					listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
					repository,
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				).toFuture().get()
			playbackEngine!!.playToCompletion().toFuture().get()
			nowPlaying = repository.nowPlaying.toFuture().get()
		}
	}

	@Test
	fun thenNowPlayingIsSetToNotRepeating() {
		assertThat(nowPlaying!!.isRepeating).isFalse
	}
}
