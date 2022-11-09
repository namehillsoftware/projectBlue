package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPlaybackIsPausedAndRestarted {
	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library()
		library.setId(1)
		val libraryProvider = PassThroughSpecificLibraryProvider(library)
		val libraryStorage = PassThroughLibraryStorage()
		val nowPlayingRepository =
			NowPlayingRepository(
				libraryProvider,
				libraryStorage,
				FakeNowPlayingState(),
			)
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider
			) { 1 }, listOf(CompletingFileQueueProvider()),
			nowPlayingRepository,
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Triple(fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine)
	}

	private val changedFiles: MutableList<ServiceFile> = ArrayList()
	private var nowPlaying: NowPlaying? = null
	private var resolvablePlaybackHandler: ResolvablePlaybackHandler? = null
	private var playbackStartedCount = 0

	@BeforeAll
	fun before() {
		val (fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine) = mut
		playbackEngine
			.setOnPlaybackStarted { ++playbackStartedCount }
			.setOnPlayingFileChanged { f -> changedFiles.add(f.serviceFile) }
		playbackEngine
			.startPlaylist(
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(3),
					ServiceFile(4),
					ServiceFile(5)
				), 0, Duration.ZERO
			)
		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
		resolvablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
		playingPlaybackHandler.resolve()
		resolvablePlaybackHandler?.setCurrentPosition(30)
		playbackEngine.pause().toExpiringFuture().get()
		nowPlaying = nowPlayingRepository.promiseNowPlaying().toExpiringFuture().get()
		playbackEngine.resume().toExpiringFuture().get()
		nowPlaying = nowPlayingRepository.promiseNowPlaying().toExpiringFuture().get()
	}

	@Test
	fun `then the player is not playing`() {
		assertThat(resolvablePlaybackHandler!!.isPlaying).isTrue
	}

	@Test
	fun `then the playback state is not playing`() {
		assertThat(mut.third.isPlaying).isTrue
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
	fun `then the changed files are correct`() {
		assertThat(changedFiles).containsExactly(
			ServiceFile(1),
			ServiceFile(2),
			ServiceFile(2)
		)
	}

	@Test
	fun `then playback has been started twice`() {
		assertThat(playbackStartedCount).isEqualTo(2)
	}

	@Test
	fun `then the saved playlist is correct`() {
		assertThat(nowPlaying!!.playlist)
			.containsExactly(
				ServiceFile(1),
				ServiceFile(2),
				ServiceFile(3),
				ServiceFile(4),
				ServiceFile(5)
			)
	}
}
