package com.lasthopesoftware.bluewater.client.playback.engine.GivenANewPlaybackEngine.AndAnEmptyPlaylist

import com.lasthopesoftware.EmptyUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenRestoringEngineStateAndResumingPlayback {
	private val restoredState by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library()
		library.setId(598)
		library.setNowPlayingProgress(543)
		library.setNowPlayingId(586)

		val libraryProvider = FakeLibraryRepository(library)

		val filePropertiesContainerRepository = mockk<IFilePropertiesContainerRepository>()
		every {
			filePropertiesContainerRepository.getFilePropertiesContainer(
				UrlKeyHolder(
					EmptyUrl.url,
					ServiceFile(4)
				)
			)
		} returns FilePropertiesContainer(1, mapOf(Pair(KnownFileProperties.Duration, "100")))

		val repository =
			NowPlayingRepository(
				libraryProvider,
				libraryProvider,
				FakeNowPlayingState(),
			)
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
			listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
			repository,
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		val restoredState = playbackEngine.restoreFromSavedState(library.libraryId).toExpiringFuture().get()

		restoredState
	}

	@Test
	fun `then the restored state is null`() {
		assertThat(restoredState).isNull()
	}
}
