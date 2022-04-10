package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaylistEngine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenMovingTracksFromBeforeToAfterPlayingTrack {

	companion object {
		private val updatedNowPlaying by lazy {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			val libraryProvider = mockk<ISpecificLibraryProvider>().apply {
				val storedLibrary by lazy {
					Library()
						.setId(1)
						.setSavedTracksString(
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
						.setNowPlayingId(3)
				}
				every { library } returns Promise(storedLibrary)
			}

			val playbackEngine = PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider) { 1 },
				listOf(CompletingFileQueueProvider()),
				NowPlayingRepository(
					libraryProvider,
					PassThroughLibraryStorage()
				),
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)

			playbackEngine.restoreFromSavedState().toExpiringFuture().get()
			playbackEngine.moveFile(1, 4).toExpiringFuture()[1, TimeUnit.SECONDS]
		}
	}

	@Test
	fun `then the playlist is updated`() {
		assertThat(updatedNowPlaying?.playlist).isEqualTo(
			listOf(
				ServiceFile(1),
				ServiceFile(3),
				ServiceFile(4),
				ServiceFile(5),
				ServiceFile(2),
			)
		)
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(updatedNowPlaying?.playlistPosition).isEqualTo(2)
	}
}
