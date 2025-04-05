package com.lasthopesoftware.bluewater.client.playback.engine.GivenANewPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenRestoringEngineStateAndResumingPlayback {
	private var positionedPlayingFile: PositionedPlayingFile? = null
	private val restoredState by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
			listOf(
				ServiceFile("32"),
				ServiceFile("329"),
				ServiceFile("580"),
				ServiceFile("915"),
				ServiceFile("899")
			)
		)
		val library = Library(
			id = 35,
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
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
			listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
			repository,
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)
		val restoredState = playbackEngine.restoreFromSavedState(library.libraryId).toExpiringFuture().get()

		playbackEngine.setOnPlayingFileChanged { _, c -> positionedPlayingFile = c	}
		val promisedResumption = playbackEngine.resume()
		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("915")]?.resolve()
		promisedResumption.toExpiringFuture().get()

		restoredState
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
