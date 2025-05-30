package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndPlaybackIsPaused.AndTheTrackIsChanged

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Playback Is Resumed` {

	companion object {
		private const val libraryId = 13
	}

	private val mut by lazy {
		val playlist = listOf(
			ServiceFile("1"),
			ServiceFile("2"),
			ServiceFile("3"),
			ServiceFile("4"),
			ServiceFile("5")
		)

		val library = Library(id = libraryId)
		val libraryProvider = FakeLibraryRepository(library)
		val nowPlayingRepository =
            NowPlayingRepository(
                FakeSelectedLibraryProvider(),
                libraryProvider,
            )
		val preparedPlaybackQueueResourceManagement =
            PreparedPlaybackQueueResourceManagement(
                mockk {
					every { providePlayableFilePreparationSource() } returns mockk {
						every { promisePreparedPlaybackFile(LibraryId(libraryId), any(), any()) } answers {
							val prepareAt = thirdArg<Duration>()
							preparedAt = prepareAt
							val playbackHandler = ResolvablePlaybackHandler()
							playbackHandler.setCurrentPosition(prepareAt.millis.toInt())

							FakePreparedPlayableFile(playbackHandler).toPromise()
						}

						val firstPlaybackHandler = ResolvablePlaybackHandler()
						firstPlaybackHandler.setCurrentPosition(450)
						every { promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile("1"), Duration.ZERO) } returns FakePreparedPlayableFile(firstPlaybackHandler).toPromise()
					}
				},
                FakePlaybackQueueConfiguration(maxQueueSize = 0)
            )
		val playbackBootstrapper = ManagedPlaylistPlayer(
            PlaylistVolumeManager(1.0f),
            preparedPlaybackQueueResourceManagement,
            nowPlayingRepository,
            listOf(CompletingFileQueueProvider()),
        )
		val playbackEngine =
            PlaybackEngine(
                preparedPlaybackQueueResourceManagement,
                listOf(CompletingFileQueueProvider()),
                nowPlayingRepository,
                playbackBootstrapper,
                playbackBootstrapper,
            )
		Triple(playlist, nowPlayingRepository, playbackEngine)
	}

	private var preparedAt: Duration? = null
	private var nowPlaying: NowPlaying? = null
	private var positionedFiles: MutableList<PositionedPlayingFile?>? = null

	@BeforeAll
	fun before() {
		val (playlist, nowPlayingRepository, playbackEngine) = mut

		val collectedFiles = mutableListOf<PositionedPlayingFile?>()

		playbackEngine.setOnPlayingFileChanged { _, f ->
			collectedFiles.add(f)
		}

		playbackEngine
			.startPlaylist(
                LibraryId(libraryId),
				playlist,
				0
			)
			.toExpiringFuture()
			.get()

		playbackEngine.pause().toExpiringFuture().get()

		playbackEngine.skipToNext().toExpiringFuture().get()

		val deferredResume = DeferredPromise(Unit)
		val promisedCollectedFiles = Promise {
			playbackEngine.setOnPlayingFileChanged { _, f ->
				collectedFiles.add(f)

				deferredResume.then { _ -> it.sendResolution(collectedFiles) }
			}
		}

		playbackEngine.resume().toExpiringFuture().get()
		deferredResume.resolve()

		positionedFiles = promisedCollectedFiles.toExpiringFuture().get()

		nowPlaying = nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the playback state is playing`() {
		assertThat(mut.third.isPlaying).isTrue
	}

	@Test
	fun `then the saved playlist position is correct`() {
		assertThat(nowPlaying?.playlistPosition).isEqualTo(1)
	}

	@Test
	fun `then the saved file position is correct`() {
		assertThat(nowPlaying?.filePosition).isEqualTo(0)
	}

	@Test
	fun `then the saved playlist is correct`() {
		assertThat(nowPlaying?.playlist)
			.containsExactly(
                ServiceFile("1"),
                ServiceFile("2"),
                ServiceFile("3"),
                ServiceFile("4"),
                ServiceFile("5")
			)
	}

	@Test
	fun `then the observed file is correct`() {
		assertThat(positionedFiles?.last()?.playlistPosition).isEqualTo(1)
	}

	@Test
	fun `then the file is prepared at the correct position`() {
		assertThat(preparedAt).isEqualTo(Duration.ZERO)
	}

	@Test
	fun `then the observed file position is correct`() {
		assertThat(
            positionedFiles?.last()?.playingFile?.progress?.toExpiringFuture()?.get()
        ).isEqualTo(Duration.ZERO)
	}

	@Test
	fun `then the observed playlist position is correct`() {
		assertThat(positionedFiles?.last()?.playlistPosition).isEqualTo(1)
	}

	@Test
	fun `then the first skipped file is only observed once`() {
		assertThat(positionedFiles?.map { it?.asPositionedFile() })
			.containsOnlyOnce(PositionedFile(1, ServiceFile("2")))
	}
}
