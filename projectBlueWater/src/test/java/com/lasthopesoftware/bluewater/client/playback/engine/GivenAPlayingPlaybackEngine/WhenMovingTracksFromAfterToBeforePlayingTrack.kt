package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenMovingTracksFromAfterToBeforePlayingTrack {

	companion object {
		private const val libraryId = 518
	}

	private val mutt by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
			listOf(
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("3"),
				ServiceFile("4"),
				ServiceFile("5")
			)
		)

		val storedLibrary =
			Library(
				id = libraryId,
				savedTracksString = FileStringListUtilities.promiseSerializedFileStringList(
					listOf(
						ServiceFile("1"),
						ServiceFile("2"),
						ServiceFile("3"),
						ServiceFile("4"),
						ServiceFile("5")
					)
				).toExpiringFuture().get(),
				nowPlayingId = 0,
			)

		val libraryProvider = FakeLibraryRepository(storedLibrary)

		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
		val nowPlayingRepository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			libraryProvider,
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			nowPlayingRepository,
			listOf(CompletingFileQueueProvider()),
		)
		Pair(
			fakePlaybackPreparerProvider,
			PlaybackEngine(
				preparedPlaybackQueueResourceManagement,
				listOf(CompletingFileQueueProvider()),
				nowPlayingRepository,
				playbackBootstrapper,
				playbackBootstrapper,
			)
		)
	}

	private var nextPlayedFile: PositionedPlayingFile? = null
	private var updatedNowPlaying: NowPlaying? = null

	@BeforeAll
	fun act() {
		val (playbackPreparer, playbackEngine) = mutt

		playbackEngine.restoreFromSavedState(LibraryId(libraryId)).toExpiringFuture().get()
		playbackEngine.resume().toExpiringFuture().get()

		val playbackHandler = playbackPreparer.deferredResolutions.values.first().resolve()
		updatedNowPlaying = playbackEngine.moveFile(2, 0).toExpiringFuture().get()

		val promisedFileChange = Promise {
			playbackEngine.setOnPlayingFileChanged { _, pf ->
				if (pf?.serviceFile == ServiceFile("2"))
					it.sendResolution(pf)
			}
		}

		playbackHandler.resolve()
		playbackPreparer.deferredResolutions[ServiceFile("2")]?.resolve()
		nextPlayedFile = promisedFileChange.toExpiringFuture().get()
	}

	@Test
	fun `then the playlist is updated`() {
		assertThat(updatedNowPlaying?.playlist).isEqualTo(
			listOf(
				ServiceFile("3"),
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("4"),
				ServiceFile("5")
			)
		)
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(updatedNowPlaying?.playlistPosition).isEqualTo(1)
	}

	@Test
	fun `then the next played file is correct`() {
		assertThat(nextPlayedFile?.asPositionedFile()).isEqualTo(PositionedFile(2, ServiceFile("2")))
	}
}
