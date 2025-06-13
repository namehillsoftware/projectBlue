package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.LinkedOnPlayingFileChangedListener
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.LockingNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.ForwardedResponse.Companion.thenForward
import com.lasthopesoftware.promises.PromiseDelay
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
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
		val deferredPlaybackHandlers = listOf(
			ServiceFile("1"),
			ServiceFile("2"),
			ServiceFile("3"),
			ServiceFile("4"),
			ServiceFile("5")
		).associateWith {
			val playbackHandler = ResolvablePlaybackHandler()
			Pair(playbackHandler, DeferredPromise(FakePreparedPlayableFile(playbackHandler)))
		}

		val library = Library(id = libraryId)
		val libraryProvider = FakeLibraryRepository(library)
		val preparedPlaybackQueueResourceManagement = PreparedPlaybackQueueResourceManagement(
			mockk {
				every { providePlayableFilePreparationSource() } returns mockk {
					every { promisePreparedPlaybackFile(LibraryId(libraryId), any(), any()) }  answers {
						deferredPlaybackHandlers[secondArg()]?.second.keepPromise().thenForward()
					}
				}
			},
			FakePlaybackQueueConfiguration()
		)
		val repository = LockingNowPlayingRepository(
			NowPlayingRepository(
				FakeSelectedLibraryProvider(),
				libraryProvider,
			)
		)
		repository.open()
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
		Triple(deferredPlaybackHandlers, repository, playbackEngine)
	}

	private var nextSwitchedFile: PositionedFile? = null
	private var latestFile: PositionedPlayingFile? = null
	private val startedFiles = Collections.synchronizedList(ArrayList<PositionedFile?>())

	@BeforeAll
	fun act() {
		val (preparedFiles, repository, playbackEngine) = mut

		var onPlayingFileChanged = OnPlayingFileChanged { _, p -> startedFiles.add(p?.asPositionedFile()) }

		val promisedFirstFile = Promise {
			onPlayingFileChanged = LinkedOnPlayingFileChangedListener(onPlayingFileChanged) { _, p ->
				if (p?.playlistPosition == 1)
					it.sendResolution(p)
			}
		}

		val promisedFinalFile = Promise {
			onPlayingFileChanged = LinkedOnPlayingFileChangedListener(onPlayingFileChanged) { _, p ->
				if (p?.playlistPosition == 4)
					it.sendResolution(p)
			}
		}

		val playlist = preparedFiles.keys.toList()

		val promisedStart = playbackEngine
			.setOnPlayingFileChanged(onPlayingFileChanged)
			.startPlaylist(
				LibraryId(libraryId),
				playlist,
				1
			)

		val (playingPlaybackHandler, resolvablePlaybackHandler) = preparedFiles.getValue(ServiceFile("2"))
		resolvablePlaybackHandler.resolve()
		promisedStart.toExpiringFuture().get()
		promisedFirstFile.toExpiringFuture().get()

		repository.close()

		val promisedChanges = Promise.whenAll(
			playbackEngine.changePosition(0, Duration.ZERO),
			playbackEngine.changePosition(2, Duration.ZERO),
			playbackEngine.changePosition(4, Duration.ZERO),
		)

		// Resolve the skipped tracks as well to ensure that they aren't the last switched track
		preparedFiles[playlist[2]]?.second?.resolve()

		val finalPreparableFile = preparedFiles[playlist[4]]
		finalPreparableFile?.second?.resolve()

		repository.open()

		nextSwitchedFile = promisedChanges.toExpiringFuture().get()?.lastOrNull()?.second

		// Resolve the first skipped track afterward to ensure a cancellation is tested.
		preparedFiles[playlist[0]]?.second?.resolve()

		playingPlaybackHandler.resolve()

		latestFile = Promise.whenAny(
			promisedFinalFile,
			PromiseDelay.delay(Duration.standardSeconds(10))
		).toFuture().get()
	}

	@Test
	fun `then the engine is playing`() {
		assertThat(mut.third.isPlaying).isTrue
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
			.startsWith(
				PositionedFile(1, ServiceFile("2")),
			)
			.endsWith(
				PositionedFile(4, ServiceFile("5")),
			)
	}
}
