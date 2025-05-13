package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndPlaybackErrors

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
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
		val fakePlaybackPreparerProvider = mockk<IPlayableFilePreparationSourceProvider> {
			every { providePlayableFilePreparationSource() } returns mockk {
				every { promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile("1"), Duration.ZERO) } returns
					FakePreparedPlayableFile(
						ResolvablePlaybackHandler().apply { resolve() }
					).toPromise()

				every { promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile("2"), Duration.ZERO) } returns
					FakePreparedPlayableFile(
						ResolvablePlaybackHandler().apply { resolve() }
					).toPromise()

				every { promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile("3"), Duration.ZERO) } returns
					FakePreparedPlayableFile(
						ResolvablePlaybackHandler().apply {
							setCurrentPosition(164)
							reject(Exception("f"))
						}
					).toPromise()
			} andThen finalPlaybackPreparer

			every { maxQueueSize } returns 0
		}

		val library = Library(id = libraryId)
		val libraryProvider = FakeLibraryRepository(library)

		val nowPlayingRepository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			libraryProvider,
		)

		val preparedPlaybackQueueResourceManagement = PreparedPlaybackQueueResourceManagement(
			fakePlaybackPreparerProvider,
			fakePlaybackPreparerProvider
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			nowPlayingRepository,
			listOf(CompletingFileQueueProvider()),
		)
		val playbackEngine = PlaybackEngine(
			preparedPlaybackQueueResourceManagement,
			listOf(CompletingFileQueueProvider()),
			nowPlayingRepository,
			playbackBootstrapper,
			playbackBootstrapper,
		)

		Pair(nowPlayingRepository, playbackEngine)
	}

	private var error: PlaybackException? = null
	private var nowPlaying: NowPlaying? = null
	private var positionedPlayingFile: PositionedPlayingFile? = null
	private var isPlayingBeforeResume = true

	@BeforeAll
	fun act() {
		val (nowPlayingRepository, playbackEngine) = mut

		val promisedError = Promise {
			playbackEngine
				.setOnPlaylistError { e ->
					if (e is PlaybackException) {
						it.sendResolution(e)
					} else {
						it.sendRejection(e)
					}
				}
		}

		playbackEngine
			.startPlaylist(
				LibraryId(libraryId),
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				),
				0
			)
			.toExpiringFuture()
			.get()

		error = promisedError.toExpiringFuture().get()
		nowPlaying = nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)).toExpiringFuture().get()
		isPlayingBeforeResume = playbackEngine.isPlaying

		val promisedFile = Promise {
			playbackEngine
				.setOnPlayingFileChanged { _, p ->
					it.sendResolution(p)
				}

			playbackEngine.resume().toExpiringFuture().get()
			finalPlaybackPreparer.resolve()
		}

		positionedPlayingFile = promisedFile.toExpiringFuture().get()
	}

	@Test
	fun `then the error is broadcast`() {
		assertThat(error?.playbackHandler?.progress?.toExpiringFuture()?.get()?.millis).isEqualTo(164)
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
