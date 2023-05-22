package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndAPreparationErrorOccurs

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
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
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 668

class WhenObservingPlayback {
	private val mut by lazy {
		val deferredErrorPlaybackPreparer = DeferredErrorPlaybackPreparer()
		val fakePlaybackPreparerProvider: IPlayableFilePreparationSourceProvider =
			object : IPlayableFilePreparationSourceProvider {
				override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource {
					return deferredErrorPlaybackPreparer
				}

                override val maxQueueSize: Int
                    get() {
                        return 1
                    }
			}

		val library = Library()
		library.setId(libraryId)
		val libraryProvider = FakeLibraryRepository(library)

		val nowPlayingRepository = NowPlayingRepository(
			libraryProvider,
			libraryProvider,
			FakeNowPlayingState(),
		)

		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				fakePlaybackPreparerProvider
			), listOf(CompletingFileQueueProvider()),
			nowPlayingRepository,
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Triple(deferredErrorPlaybackPreparer, nowPlayingRepository, playbackEngine)
	}

	private var error: PreparationException? = null
	private var nowPlaying: NowPlaying? = null

	@BeforeAll
	fun act() {
		val (deferredErrorPlaybackPreparer, nowPlayingRepository, playbackEngine) = mut

		playbackEngine
			.setOnPlaylistError { e ->
				if (e is PreparationException) error = e
			}
			.startPlaylist(
				LibraryId(libraryId),
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
			.toExpiringFuture()
			.get()
		deferredErrorPlaybackPreparer.resolve().resolve()
		deferredErrorPlaybackPreparer.reject()
		nowPlaying = nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the error is broadcast`() {
		assertThat(error).isNotNull
	}

	@Test
	fun `then the file position is saved`() {
		assertThat(nowPlaying!!.playlistPosition).isEqualTo(1)
	}

	private class DeferredErrorPlaybackPreparer : PlayableFilePreparationSource {
		private var messenger: Messenger<PreparedPlayableFile?>? = null
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
			libraryId: LibraryId,
			serviceFile: ServiceFile,
			preparedAt: Duration
		): Promise<PreparedPlayableFile?> {
			return Promise { messenger ->
				this.messenger = messenger
			}
		}
	}
}
