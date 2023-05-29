package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
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
import io.mockk.every
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

private const val libraryId = 411

class WhenRemovingFilesBeforeTheCurrentlyPlayingFile {

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()

		val nowPlayingState = FakeNowPlayingState()
		val repository =
			NowPlayingRepository(
				FakeSelectedLibraryProvider(),
				libraryProvider,
				libraryProvider,
				nowPlayingState,
			)
		val playbackEngine =
			PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
				listOf(spyk(CompletingFileQueueProvider()).apply {
					every { provideQueue(any(), any(), any()) } answers {
						queueStart = lastArg()
						callOriginal()
					}
				}),
				repository,
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
		Triple(fakePlaybackPreparerProvider, repository, playbackEngine)
	}

	private val library by lazy {
		Library()
			.setId(libraryId)
			.setSavedTracksString(
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
			.setNowPlayingProgress(35)
			.setNowPlayingId(2)
	}
	private val libraryProvider by lazy { FakeLibraryRepository(library) }
	private var initialState: PositionedProgressedFile? = null
	private var queueStart = 0
	private var nowPlaying: NowPlaying? = null

	@BeforeAll
	fun before() {
		val (fakePlaybackPreparerProvider, repository, playbackEngine) = mut

		initialState = playbackEngine.restoreFromSavedState(LibraryId(libraryId)).toExpiringFuture().get()
		playbackEngine.resume().toExpiringFuture()[1, TimeUnit.SECONDS]

		val resolvablePlaybackHandler =	fakePlaybackPreparerProvider.deferredResolution.resolve()
		playbackEngine.removeFileAtPosition(0).toExpiringFuture()[1, TimeUnit.SECONDS]
		resolvablePlaybackHandler.setCurrentPosition(92)
		playbackEngine.pause().toExpiringFuture().get()

		nowPlaying = repository.promiseNowPlaying(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the currently playing file shifts`() {
		assertThat(libraryProvider.promiseLibrary(LibraryId(libraryId)).toExpiringFuture().get()!!.nowPlayingId).isEqualTo(1)
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
