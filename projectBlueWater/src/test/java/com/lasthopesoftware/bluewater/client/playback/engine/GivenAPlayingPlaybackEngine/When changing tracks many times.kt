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
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Collections

class `When changing tracks many times` {

	companion object {
		private const val libraryId = 639
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
	private val startedFiles = Collections.synchronizedList(ArrayList<PositionedFile?>())

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, playbackEngine) = mut

		val promisedFinalFile = Promise {
			playbackEngine
				.setOnPlayingFileChanged { _, p ->
					startedFiles.add(p?.asPositionedFile())

					if (p?.playlistPosition == 4)
						it.sendResolution(p)
				}
		}

		val promisedStart = playbackEngine
			.startPlaylist(
				LibraryId(libraryId),
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				),
				1
			)

		val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()
		promisedStart.toExpiringFuture().get()

		val promisedChanges = Promise.whenAll(
			playbackEngine.changePosition(0, Duration.ZERO),
			playbackEngine.changePosition(2, Duration.ZERO),
			playbackEngine.changePosition(4, Duration.ZERO),
		)

		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("3")]?.resolve()
		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()
		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("5")]?.resolve()

		nextSwitchedFile = promisedChanges.toExpiringFuture().get()?.lastOrNull()?.second

		playingPlaybackHandler?.resolve()

		latestFile = promisedFinalFile.toExpiringFuture().get()
	}

	@Test
	fun `then the engine is playing`() {
		assertThat(mut.second.isPlaying).isTrue
	}

	@Test
	fun `then the next file change is the switched to the correct track position`() {
		assertThat(nextSwitchedFile?.playlistPosition).isEqualTo(4)
	}

	@Test
	fun `then the latest observed file is at the correct track position`() {
		assertThat(latestFile?.playlistPosition).isEqualTo(4)
	}

	@Test
	fun `then the latest observed file is at the correct file position`() {
		assertThat(latestFile?.playingFile?.progress?.toExpiringFuture()?.get()).isEqualTo(Duration.ZERO)
	}

	@Test
	fun `then the started files are correct`() {
		assertThat(startedFiles)
			.containsExactly(
				PositionedFile(1, ServiceFile("2")),
				PositionedFile(4, ServiceFile("5")),
			)
	}
}
