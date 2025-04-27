package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndPlaybackIsInterrupted

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Playback Is Resumed` {

	companion object {
		private const val libraryId = 852
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
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
			listOf(CompletingFileQueueProvider()),
			nowPlayingRepository,
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Triple(fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine)
	}

	private var isInterrupted = false
	private var nowPlaying: NowPlaying? = null
	private var resolvablePlaybackHandler: ResolvablePlaybackHandler? = null

	@BeforeAll
	fun before() {
		val (fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine) = mut
		playbackEngine
			.startPlaylist(
				LibraryId(libraryId),
				fakePlaybackPreparerProvider.deferredResolutions.keys.toList(),
				0,
				Duration.ZERO
			)
		playbackEngine.setOnPlaybackInterrupted { isInterrupted = true }

		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()
		resolvablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()
		playingPlaybackHandler?.resolve()
		resolvablePlaybackHandler?.setCurrentPosition(30)
		playbackEngine.interrupt().toExpiringFuture().get()
		nowPlaying = nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)).toExpiringFuture().get()
		playbackEngine.resume().toExpiringFuture().get()
	}

	@Test
	fun `then the player is playing because interrupt is supposed to mean a momentarily INTERRUPTION of playback, not cancellation`() {
		assertThat(resolvablePlaybackHandler?.isPlaying).isTrue
	}

	@Test
	fun `then the playback state is not playing`() {
		assertThat(mut.third.isPlaying).isTrue
	}

	@Test
	fun `then the saved file position is correct`() {
		assertThat(nowPlaying?.filePosition).isEqualTo(30)
	}

	@Test
	fun `then the saved playlist position is correct`() {
		assertThat(nowPlaying?.playlistPosition).isEqualTo(1)
	}

	@Test
	fun `then playback is reported as interrupted`() {
		assertThat(isInterrupted).isTrue
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
}
