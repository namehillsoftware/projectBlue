package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaylistEngine

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
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

class WhenSettingEngineToRepeat {
	@Test
	fun thenNowPlayingIsSetToRepeating() {
		Assertions.assertThat(nowPlaying!!.isRepeating).isTrue
	}

	companion object {
		private val library = Library()
		private var nowPlaying: NowPlaying? = null
		@BeforeClass
		@JvmStatic
		@Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
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
			val libraryPromise = library.toPromise()
			val libraryProvider = object : ISpecificLibraryProvider {
				override val library: Promise<Library?>
					get() = libraryPromise.then { it }
			}

			val libraryStorage = PassThroughLibraryStorage()

			val filePropertiesContainerRepository = mock(
				IFilePropertiesContainerRepository::class.java
			)
			`when`(
				filePropertiesContainerRepository.getFilePropertiesContainer(
					UrlKeyHolder(
						URL(""),
						ServiceFile(4)
					)
				)
			).thenReturn(FilePropertiesContainer(1, mapOf(Pair(KnownFileProperties.DURATION, "100"))))
			val repository = NowPlayingRepository(libraryProvider, libraryStorage)
			val playbackEngine = FuturePromise(
				createEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 },
					Arrays.asList(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
					repository,
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				)
			).get()
			playbackEngine!!.playRepeatedly()
			nowPlaying = FuturePromise(repository.nowPlaying).get()
		}
	}
}
