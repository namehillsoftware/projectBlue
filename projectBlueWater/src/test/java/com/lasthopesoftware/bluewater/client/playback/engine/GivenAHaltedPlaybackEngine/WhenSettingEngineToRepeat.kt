package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaybackEngine

import com.lasthopesoftware.EmptyUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenSettingEngineToRepeat {
	private val nowPlaying by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library()
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
			).toExpiringFuture().get()
		)
		library.setNowPlayingId(0)
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

		val repository =
			NowPlayingRepository(
				libraryProvider,
				libraryStorage
			)
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider
			) { 1 },
			listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
			repository,
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f)))
		playbackEngine.restoreFromSavedState()
		playbackEngine.playRepeatedly().toExpiringFuture().get()
		repository.promiseNowPlaying().toExpiringFuture().get()
	}

	@Test
	fun `then now playing is set to repeating`() {
		assertThat(nowPlaying?.isRepeating).isTrue
	}
}
