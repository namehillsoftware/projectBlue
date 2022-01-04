package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndAPreparationErrorOccurs

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenObservingPlayback {
	@Test
	fun thenTheErrorIsBroadcast() {
		assertThat(error).isNotNull
	}

	@Test
	fun thenTheFilePositionIsSaved() {
		assertThat(nowPlaying!!.playlistPosition).isEqualTo(1)
	}

	private class DeferredErrorPlaybackPreparer : PlayableFilePreparationSource {
		private var messenger: Messenger<PreparedPlayableFile>? = null
		fun resolve(): ResolvablePlaybackHandler {
			val playbackHandler = ResolvablePlaybackHandler()
			messenger?.sendResolution(
				FakePreparedPlayableFile(
					playbackHandler
				)
			)
			return playbackHandler
		}

		fun reject() {
			messenger?.sendRejection(Exception())
		}

		override fun promisePreparedPlaybackFile(
			serviceFile: ServiceFile,
			preparedAt: Duration
		): Promise<PreparedPlayableFile> {
			return Promise { messenger: Messenger<PreparedPlayableFile>? ->
				this.messenger = messenger
			}
		}
	}

	companion object {
		private var error: PreparationException? = null
		private var nowPlaying: NowPlaying? = null

		@BeforeClass
		@JvmStatic
		fun context() {
			val deferredErrorPlaybackPreparer = DeferredErrorPlaybackPreparer()
			val fakePlaybackPreparerProvider: IPlayableFilePreparationSourceProvider =
				object : IPlayableFilePreparationSourceProvider {
					override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource {
						return deferredErrorPlaybackPreparer
					}

					override fun getMaxQueueSize(): Int {
						return 1
					}
				}
			val library = Library()
			library.setId(1)
			val libraryProvider = mockk<ISpecificLibraryProvider>()
			every { libraryProvider.library } returns Promise(library)
			val libraryStorage: ILibraryStorage = PassThroughLibraryStorage()
			val nowPlayingRepository = NowPlayingRepository(libraryProvider, libraryStorage)
			PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(
					fakePlaybackPreparerProvider,
					fakePlaybackPreparerProvider
				), listOf(CompletingFileQueueProvider()),
				nowPlayingRepository,
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
				.setOnPlaylistError { e ->
					if (e is PreparationException) error = e
				}
				.startPlaylist(
					listOf(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(3),
						ServiceFile(4),
						ServiceFile(5)
					),
					0,
					Duration.ZERO
				)
				.toFuture()
				.get()
			deferredErrorPlaybackPreparer.resolve().resolve()
			deferredErrorPlaybackPreparer.reject()
			nowPlaying = FuturePromise(nowPlayingRepository.nowPlaying).get()
		}
	}
}
