package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenRemovingFilesBeforeTheCurrentlyPlayingFile {

	private val mut by lazy {
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
		library.setNowPlayingProgress(35)
		library.setNowPlayingId(2)

		val libraryProvider = mockk<ISpecificLibraryProvider>()
		every { libraryProvider.library } returns Promise(library)

		val libraryStorage = PassThroughLibraryStorage()

		val nowPlayingState = FakeNowPlayingState()
		val repository =
			NowPlayingRepository(
				libraryProvider,
				libraryStorage,
				nowPlayingState,
			)
		val playbackEngine =
			PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(
					fakePlaybackPreparerProvider
				) { 1 },
				listOf(spyk(CompletingFileQueueProvider()).apply {
					every { provideQueue(any(), any()) } answers {
						queueStart = secondArg()
						callOriginal()
					}
				}),
				NowPlayingRepository(
					libraryProvider,
					libraryStorage,
					nowPlayingState,
				),
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
		Triple(fakePlaybackPreparerProvider, repository, playbackEngine)
	}

	private var initialState: PositionedProgressedFile? = null
	private var queueStart = 0
	private val library = Library()
	private var nowPlaying: NowPlaying? = null

	@BeforeAll
	fun before() {
		val (fakePlaybackPreparerProvider, repository, playbackEngine) = mut

		initialState = playbackEngine.restoreFromSavedState().toExpiringFuture().get()
		playbackEngine.resume().toExpiringFuture()[1, TimeUnit.SECONDS]

		val resolvablePlaybackHandler =	fakePlaybackPreparerProvider.deferredResolution.resolve()
		playbackEngine.removeFileAtPosition(0).toExpiringFuture()[1, TimeUnit.SECONDS]
		resolvablePlaybackHandler.setCurrentPosition(92)
		playbackEngine.pause().toExpiringFuture().get()

		nowPlaying = repository.promiseNowPlaying().toExpiringFuture().get()
	}

	@Test
	fun `then the currently playing file shifts`() {
		assertThat(library.nowPlayingId).isEqualTo(1)
	}

	@Test
	fun `then the file queue is shifted`() {
		assertThat(queueStart).isEqualTo(2)
	}

	@Test
	fun `then the currently playing file still tracks file progress`() {
		assertThat(nowPlaying!!.filePosition).isEqualTo(92)
	}

	@Test
	fun `then the initial file progress is correct`() {
		assertThat(initialState?.progress?.toExpiringFuture()?.get()?.millis).isEqualTo(35)
	}
}
