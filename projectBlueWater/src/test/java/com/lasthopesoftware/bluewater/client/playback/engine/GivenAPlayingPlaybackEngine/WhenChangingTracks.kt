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
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WhenChangingTracks {

	companion object {
		private const val libraryId = 265
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
		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
		val repository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			libraryProvider,
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			repository,
			listOf(CompletingFileQueueProvider()),
		)
		val playbackEngine =
			PlaybackEngine(
				preparedPlaybackQueueResourceManagement,
				listOf(CompletingFileQueueProvider()),
				repository,
				playbackBootstrapper,
				playbackBootstrapper,
			)
		Pair(fakePlaybackPreparerProvider, playbackEngine)
	}

	private var nextSwitchedFile: PositionedFile? = null
	private var latestFile: PositionedPlayingFile? = null
	private val startedFiles = Collections.synchronizedList(ArrayList<PositionedPlayingFile?>())

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, playbackEngine) = mut

		val countDownLatch = CountDownLatch(2)
		val promisedStart = playbackEngine
			.setOnPlayingFileChanged { _, p ->
				startedFiles.add(p)
				latestFile = p
				countDownLatch.countDown()
			}
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

		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()
		promisedStart.toExpiringFuture().get()

		val futurePositionChange = playbackEngine.changePosition(3, Duration.ZERO).toExpiringFuture()
		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("4")]?.resolve()
		playingPlaybackHandler?.resolve()

		if (!countDownLatch.await(10, TimeUnit.SECONDS)) Assertions.fail<Unit>("Timed out waiting for file change")
		nextSwitchedFile = futurePositionChange.get()?.second
	}

	@Test
	fun `then the engine is playing`() {
		assertThat(mut.second.isPlaying).isTrue
	}

	@Test
	fun `then the next file change is the switched to the correct track position`() {
		assertThat(nextSwitchedFile?.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the latest observed file is at the correct track position`() {
		assertThat(latestFile?.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the first started file is correct`() {
		assertThat(startedFiles[0]?.asPositionedFile())
			.isEqualTo(PositionedFile(0, ServiceFile("1")))
	}

	@Test
	fun `then the changed started file is correct`() {
		assertThat(startedFiles[1]?.asPositionedFile())
			.isEqualTo(PositionedFile(3, ServiceFile("4")))
	}

	@Test
	fun `then the playlist is started twice`() {
		assertThat(startedFiles).hasSize(2)
	}
}
