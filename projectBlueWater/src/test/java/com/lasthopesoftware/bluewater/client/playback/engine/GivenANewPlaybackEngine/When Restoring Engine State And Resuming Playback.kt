package com.lasthopesoftware.bluewater.client.playback.engine.GivenANewPlaybackEngine

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
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Restoring Engine State And Resuming Playback` {
	companion object {
		private val libraryId = 35
	}

	private val mutt by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
			listOf(
				ServiceFile("32"),
				ServiceFile("329"),
				ServiceFile("580"),
				ServiceFile("915"),
				ServiceFile("899")
			)
		)

		fakePlaybackPreparerProvider.preparationSourceBeingProvided { serviceFile, deferredPreparedPlayableFile ->
			if (serviceFile == ServiceFile("915"))
				deferredPreparedPlayableFile.resolve()
		}

		val library = Library(
			id = libraryId,
			savedTracksString = FileStringListUtilities.promiseSerializedFileStringList(
				fakePlaybackPreparerProvider.deferredResolutions.keys
			).toExpiringFuture().get(),
			nowPlayingProgress = 893,
			nowPlayingId = 3,
		)

		val libraryProvider = FakeLibraryRepository(library)

		val repository =
			NowPlayingRepository(
				FakeSelectedLibraryProvider(),
				libraryProvider,
			)
		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			repository,
			listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
		)

		PlaybackEngine(
			preparedPlaybackQueueResourceManagement,
			listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
			repository,
			playbackBootstrapper,
			playbackBootstrapper,
		)
	}

	private var restoredState: Pair<LibraryId, PositionedProgressedFile?>? = null
	private var positionedPlayingFile: PositionedPlayingFile? = null

	@BeforeAll
	fun act() {
		val playbackEngine = mutt
		restoredState = playbackEngine.restoreFromSavedState(LibraryId(libraryId)).toExpiringFuture().get()

		val promisedChangedFile = Promise {
			playbackEngine.setOnPlayingFileChanged { _, c -> it.sendResolution(c) }
		}

		val promisedResumption = playbackEngine.resume()
		promisedResumption.toExpiringFuture().get()
		positionedPlayingFile = promisedChangedFile.toExpiringFuture().get()
	}

	@Test
	fun `then the playlist position is correct`() {
		assertThat(restoredState?.second?.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the service file is correct`() {
		assertThat(restoredState?.second?.serviceFile).isEqualTo(ServiceFile("915"))
	}

	@Test
	fun `then the file progress is correct`() {
		assertThat(restoredState?.second?.progress?.toExpiringFuture()?.get()?.millis).isEqualTo(893)
	}

	@Test
	fun `then the playing file playlist position is correct`() {
		assertThat(positionedPlayingFile?.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the playing file progress is correct`() {
		assertThat(positionedPlayingFile?.playingFile?.progress?.toExpiringFuture()?.get()?.millis).isEqualTo(893)
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(positionedPlayingFile?.serviceFile).isEqualTo(ServiceFile("915"))
	}
}
