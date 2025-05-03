package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndPlaybackIsPaused.AndPlaybackFinishes

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
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Playback Is Resumed` {

	companion object {
		private const val libraryId = 134
	}

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
            listOf(
                ServiceFile("1"),
                ServiceFile("2"),
                ServiceFile("3"),
                ServiceFile("4"),
                ServiceFile("5")
            )
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
                fakePlaybackPreparerProvider,
                FakePlaybackQueueConfiguration()
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
		Triple(fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine)
	}

	private var nowPlaying: NowPlaying? = null
	private var lastPlayedFile: PositionedPlayingFile? = null

	@BeforeAll
	fun before() {
		val (fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine) = mut

		val promiseLastPlayedFile = Promise {
			playbackEngine.setOnPlayingFileChanged { _, p ->
				if (p?.serviceFile == ServiceFile("2"))
					it.sendResolution(p)
			}
		}

		playbackEngine
			.startPlaylist(
                LibraryId(libraryId),
				fakePlaybackPreparerProvider.deferredResolutions.keys.toList(),
				0
			)
			.toExpiringFuture()
			.get()

		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()
		playingPlaybackHandler?.setCurrentPosition(176379)

		playbackEngine.pause().toExpiringFuture().get()
		playbackEngine.resume().toExpiringFuture().get()

		playingPlaybackHandler?.resolve()

		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()

		lastPlayedFile = promiseLastPlayedFile.toExpiringFuture().get()

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
	fun `then the last played file is correct`() {
		assertThat(lastPlayedFile?.asPositionedFile()).isEqualTo(
			PositionedFile(
				1,
				ServiceFile("2")
			)
		)
	}

	@Test
	fun `then the last played file position is correct`() {
		assertThat(lastPlayedFile?.playingFile?.progress?.toExpiringFuture()?.get()).isEqualTo(Duration.ZERO)
	}
}
