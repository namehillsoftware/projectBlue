package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
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

private const val libraryId = 678

class WhenPlaybackIsPausedAndPositionIsChangedAndRestarted {

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
		val playbackEngine =
			PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
				listOf(CompletingFileQueueProvider()),
				nowPlayingRepository,
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
		Triple(fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine)
	}

	private var nowPlaying: NowPlaying? = null
	private val positionedFiles: MutableList<PositionedPlayingFile?> = ArrayList()

	@BeforeAll
	fun before() {
		val (fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine) = mut

		val promisedStart = playbackEngine
			.setOnPlayingFileChanged { _, f -> positionedFiles.add(f) }
			.startPlaylist(
				LibraryId(libraryId),
				fakePlaybackPreparerProvider.deferredResolutions.keys.toList(),
				0,
				Duration.ZERO
			)
		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()
		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()
		playingPlaybackHandler?.resolve()
		promisedStart.toExpiringFuture().get()
		playbackEngine.pause().toExpiringFuture().get()
		nowPlaying =
			playbackEngine
				.skipToNext()
				.eventually { playbackEngine.skipToNext() }
				.then { _ -> playbackEngine.resume() }
				.then { _ -> fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("4")]?.resolve() }
				.eventually { nowPlayingRepository.promiseNowPlaying(LibraryId(libraryId)) }
				.toExpiringFuture()
				.get()
	}

	@Test
	fun `then the playback state is playing`() {
		assertThat(mut.third.isPlaying).isTrue
	}

	@Test
	fun `then the saved playlist position is correct`() {
		assertThat(nowPlaying!!.playlistPosition).isEqualTo(3)
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

	@Test
	fun `then the observed file is correct`() {
		assertThat(positionedFiles.last()?.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the first skipped file is only observed once`() {
		assertThat(
			positionedFiles
				.map { it?.asPositionedFile() })
			.containsOnlyOnce(PositionedFile(1, ServiceFile("2")))
	}

	@Test
	fun `then the second skipped file is not observed`() {
		assertThat(
			positionedFiles
				.map { it?.asPositionedFile() })
			.doesNotContain(PositionedFile(2, ServiceFile("3")))
	}
}
