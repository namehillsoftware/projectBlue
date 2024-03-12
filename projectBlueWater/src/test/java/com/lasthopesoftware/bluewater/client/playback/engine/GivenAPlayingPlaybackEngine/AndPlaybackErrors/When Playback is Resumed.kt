package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndPlaybackErrors

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
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
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 362

class `When Playback is Resumed` {
	private val finalPlaybackPreparer = FinalPlaybackPreparer()

	private val mut by lazy {
		val deferredPlaybackPreparer = DeferredPlaybackPreparer()

		val fakePlaybackPreparerProvider = mockk<IPlayableFilePreparationSourceProvider> {
			every { providePlayableFilePreparationSource() } returns deferredPlaybackPreparer andThen finalPlaybackPreparer

			every { maxQueueSize } returns 1
		}

		val library = Library(id = libraryId)
		val libraryProvider = FakeLibraryRepository(library)

		val nowPlayingRepository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			libraryProvider,
			libraryProvider,
			FakeNowPlayingState(),
		)

		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				fakePlaybackPreparerProvider
			),
			listOf(CompletingFileQueueProvider()),
			nowPlayingRepository,
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Triple(deferredPlaybackPreparer, nowPlayingRepository, playbackEngine)
	}

	private var error: PlaybackException? = null
	private var nowPlaying: NowPlaying? = null
	private var positionedPlayingFile: PositionedPlayingFile? = null
	private var isPlayingBeforeResume = true

	@BeforeAll
	fun act() {
		val (deferredErrorPlaybackPreparer, nowPlayingRepository, playbackEngine) = mut

		playbackEngine
			.setOnPlaylistError { e ->
				if (e is PlaybackException) error = e
			}
			.setOnPlayingFileChanged { _, pf ->
				positionedPlayingFile = pf
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

		deferredErrorPlaybackPreparer.resolve().resolve()
		with (deferredErrorPlaybackPreparer.resolve()) {
			setCurrentPosition(164)
			reject(Exception("f"))
		}

		nowPlaying = nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)).toExpiringFuture().get()
		isPlayingBeforeResume = playbackEngine.isPlaying

		playbackEngine.resume().toExpiringFuture().get()
		finalPlaybackPreparer.resolve()
	}

	@Test
	fun `then the error is broadcast`() {
		assertThat(error).isNotNull
	}

	@Test
	fun `then the file position is saved`() {
		assertThat(nowPlaying!!.filePosition).isEqualTo(164)
	}

	@Test
	fun `then the playlist position is saved`() {
		assertThat(nowPlaying!!.playlistPosition).isEqualTo(2)
	}

	@Test
	fun `then the resumed playback player is correct`() {
		assertThat(positionedPlayingFile?.playingFile).isEqualTo(finalPlaybackPreparer.preparedPlayableFile.playbackHandler)
	}

	@Test
	fun `then the playback engine is not playing`() {
		assertThat(isPlayingBeforeResume).isFalse()
	}

	private open class DeferredPlaybackPreparer : PlayableFilePreparationSource {
		private var messenger: Messenger<PreparedPlayableFile?>? = null
		fun resolve(): ResolvablePlaybackHandler {
			val playbackHandler = ResolvablePlaybackHandler()
			messenger?.sendResolution(FakePreparedPlayableFile(playbackHandler))
			return playbackHandler
		}

		override fun promisePreparedPlaybackFile(
			libraryId: LibraryId,
			serviceFile: ServiceFile,
			preparedAt: Duration): Promise<PreparedPlayableFile?> = Promise { messenger ->
				this.messenger = messenger
			}
	}

	private class FinalPlaybackPreparer : PlayableFilePreparationSource {

		val playbackHandler = ResolvablePlaybackHandler()

		val preparedPlayableFile = FakePreparedPlayableFile(playbackHandler)

		private var messenger: Messenger<PreparedPlayableFile?>? = null
		fun resolve(): ResolvablePlaybackHandler {
			messenger?.sendResolution(preparedPlayableFile)
			return playbackHandler
		}

		override fun promisePreparedPlaybackFile(
			libraryId: LibraryId,
			serviceFile: ServiceFile,
			preparedAt: Duration): Promise<PreparedPlayableFile?> = Promise { messenger ->
			this.messenger = messenger
		}
	}
}
