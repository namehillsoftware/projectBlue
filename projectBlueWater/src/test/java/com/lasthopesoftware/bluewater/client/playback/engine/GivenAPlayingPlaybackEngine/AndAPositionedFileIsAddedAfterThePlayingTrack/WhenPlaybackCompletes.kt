package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndAPositionedFileIsAddedAfterThePlayingTrack

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
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPlaybackCompletes {

	companion object {
		private const val libraryId = 405581
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

		Pair(fakePlaybackPreparerProvider, playbackEngine)
	}

	private var updatedAfterAdd: NowPlaying? = null
	private val observedPlayingFiles = mutableListOf<PositionedFile>()
	private var lastCompletedPlayedFile: PositionedFile? = null
	private var resetPositionedFile: PositionedFile? = null

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, playbackEngine) = mut

		val promisedSecondPlaybackHandler = Promise {
			fakePlaybackPreparerProvider.preparationSourceBeingProvided { file, deferredResolution ->
				val playbackHandler = deferredResolution.resolve()
				if (file != ServiceFile("2"))
					playbackHandler.resolve()
				else
					it.sendResolution(playbackHandler)
			}
		}

		val promisedSecondFilePlaybackStarted = Promise {
			playbackEngine
				.setOnPlayingFileChanged { _, f ->
					if (f?.serviceFile == ServiceFile("2"))
						it.sendResolution(Unit)

					f?.apply { observedPlayingFiles.add(asPositionedFile()) }?.playingFile?.promisePlayedFile()
						?.then { _ ->
							lastCompletedPlayedFile = f.asPositionedFile()
						}
				}
		}

		val promisedCompletedPlayback = object : Promise<Unit>() {
			init {
				playbackEngine
					.setOnPlaylistReset { _, f -> resetPositionedFile = f }
					.setOnPlaybackCompleted { resolve(Unit) }
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
			}
		}

		promisedSecondFilePlaybackStarted.toExpiringFuture().get()

		// Pause progression after second file starts playing

		updatedAfterAdd = playbackEngine
			.playFileNext(ServiceFile("OyA2Z4yn"))
			.toExpiringFuture()
			.get()

		promisedSecondPlaybackHandler.toExpiringFuture().get()?.resolve()

		promisedCompletedPlayback.toExpiringFuture().get()
	}

	@Test
	fun `then the playback state is not playing`() {
		assertThat(mut.second.isPlaying).isFalse
	}

	@Test
	fun `then the observed file position is correct`() {
		assertThat(observedPlayingFiles).isEqualTo(
			listOf(
				PositionedFile(0, ServiceFile("1")),
				PositionedFile(1, ServiceFile("2")),
				PositionedFile(2, ServiceFile("OyA2Z4yn")),
				PositionedFile(3, ServiceFile("3")),
				PositionedFile(4, ServiceFile("4")),
				PositionedFile(5, ServiceFile("5")),
			)
		)
	}

	@Test
	fun `then the completed played file is correct`() {
		assertThat(lastCompletedPlayedFile).isEqualTo(PositionedFile(5, ServiceFile("5")))
	}

	@Test
	fun `then the reset file position is zero`() {
		assertThat(resetPositionedFile).isEqualTo(PositionedFile(0, ServiceFile("1")))
	}

	@Test
	fun `then the saved file position is correct`() {
		assertThat(updatedAfterAdd?.filePosition).isEqualTo(0)
	}

	@Test
	fun `then the saved playlist position is correct`() {
		assertThat(updatedAfterAdd?.playlistPosition).isEqualTo(1)
	}

	@Test
	fun `then the saved playlist is correct`() {
		assertThat(updatedAfterAdd?.playlist)
			.containsExactly(
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("OyA2Z4yn"),
				ServiceFile("3"),
				ServiceFile("4"),
				ServiceFile("5")
			)
	}
}
