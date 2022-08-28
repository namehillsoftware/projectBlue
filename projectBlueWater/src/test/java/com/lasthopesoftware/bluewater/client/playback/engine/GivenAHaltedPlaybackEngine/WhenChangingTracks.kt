package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenChangingTracks {

	private val mut by lazy {
		val library = Library()

		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		library.setId(1)
		library.setSavedTracksString(
			FileStringListUtilities.promiseSerializedFileStringList(
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(3),
					ServiceFile(4),
					ServiceFile(5)
				)
			).toExpiringFuture().get()
		)
		library.setNowPlayingId(0)
		val libraryProvider = mockk<ISpecificLibraryProvider>()
		every { libraryProvider.library } returns Promise(library)

		val libraryStorage = PassThroughLibraryStorage()
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider
			) { 1 }, listOf(CompletingFileQueueProvider()),
			NowPlayingRepository(
				libraryProvider,
				libraryStorage
			),
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Pair(library, playbackEngine)
	}
	private val library: Library
		get() = mut.first
	private var initialState: PositionedProgressedFile? = null
	private var nextSwitchedFile: PositionedFile? = null

	@BeforeAll
	fun before() {
		val (_, playbackEngine) = mut
		initialState = playbackEngine.restoreFromSavedState().toExpiringFuture().get()
		nextSwitchedFile = playbackEngine.changePosition(3, Duration.ZERO).toExpiringFuture()[1, TimeUnit.SECONDS]
	}

	@Test
	fun `then the initial playlist position is correct`() {
		assertThat(initialState?.playlistPosition).isEqualTo(0)
	}

	@Test
	fun `then the next file change is the correct playlist position`() {
		assertThat(nextSwitchedFile!!.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the saved library is at the correct playlist position`() {
		assertThat(library.nowPlayingId).isEqualTo(3)
	}
}
