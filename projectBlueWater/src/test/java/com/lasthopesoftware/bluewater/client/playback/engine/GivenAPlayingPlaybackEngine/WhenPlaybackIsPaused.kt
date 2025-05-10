package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.AlwaysOpenNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.LockingNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 963

class WhenPlaybackIsPaused {

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
		val nowPlayingRepository = AlwaysOpenNowPlayingRepository(
			LockingNowPlayingRepository(
				NowPlayingRepository(
					FakeSelectedLibraryProvider(),
					libraryProvider,
				)
			)
		)
		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
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

		Triple(fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine)
	}

	private var nowPlaying: NowPlaying? = null
	private var resolvablePlaybackHandler: ResolvablePlaybackHandler? = null

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine) = mut

		val promisedPlaybackStart = playbackEngine
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

		val promisedFirstFileStart = Promise {
			playbackEngine.setOnPlayingFileChanged { _, p ->
				if (p?.serviceFile == ServiceFile("1"))
					it.sendResolution(Unit)
			}
		}

		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()

		nowPlayingRepository.open().use {
			promisedPlaybackStart.toExpiringFuture().get()
			promisedFirstFileStart.toExpiringFuture().get()
		}

		playingPlaybackHandler?.resolve()

		resolvablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()
		resolvablePlaybackHandler?.setCurrentPosition(30)

		val promisedPause = playbackEngine.pause()

		nowPlayingRepository.open()

		promisedPause.toExpiringFuture().get()

		nowPlaying = nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the player is not playing`() {
		assertThat(resolvablePlaybackHandler?.isPlaying).isFalse
	}

	@Test
	fun `then the playback state is not playing`() {
		assertThat(mut.third.isPlaying).isFalse
	}

	@Test
	fun `then the saved file position is correct`() {
		assertThat(nowPlaying!!.filePosition).isEqualTo(30)
	}

	@Test
	fun `then the saved playlist position is correct`() {
		assertThat(nowPlaying!!.playlistPosition).isEqualTo(1)
	}

	@Test
	fun `then the saved playlist is correct`() {
		assertThat(nowPlaying!!.playlist)
			.containsExactly(
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("3"),
				ServiceFile("4"),
				ServiceFile("5")
			)
	}
}
