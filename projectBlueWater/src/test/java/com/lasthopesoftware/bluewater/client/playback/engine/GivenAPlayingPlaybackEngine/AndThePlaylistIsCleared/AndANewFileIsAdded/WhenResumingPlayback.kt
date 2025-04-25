package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndThePlaylistIsCleared.AndANewFileIsAdded

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenResumingPlayback {

	private val mut by lazy {
		val storedLibrary = Library(
			id = 1,
			savedTracksString = FileStringListUtilities.promiseSerializedFileStringList(
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				)
			).toExpiringFuture().get(),
			nowPlayingId = 3,
		)

		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
			listOf(
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("3"),
				ServiceFile("4"),
				ServiceFile("5"),
				ServiceFile("701")
			)
		)
		val libraryProvider = FakeLibraryRepository(storedLibrary)

		val engine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				fakePlaybackPreparerProvider
			),
			listOf(CompletingFileQueueProvider()),
			NowPlayingRepository(
				FakeSelectedLibraryProvider(),
				libraryProvider,
			),
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		engine
			.setOnPlaybackCompleted { isPlaybackCompletionSignaled = true }
			.setOnPlayingFileChanged { _, f -> playingFiles.add(f?.serviceFile) }

		Pair(fakePlaybackPreparerProvider, engine)
	}
	private val playingFiles = ArrayList<ServiceFile?>()

	private var isPlayingAfterPlaylistCleared = false
	private var playlistAfterClearing: List<ServiceFile>? = null
	private var isPlaybackCompletionSignaled = false
	private var updatedNowPlayingAfterClearing: NowPlaying? = null

	@BeforeAll
	fun act() {
		val (provider, engine) = mut
		engine
			.startPlaylist(
				LibraryId(1),
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				),
				0,
				Duration.standardMinutes(1)
			)
			.toExpiringFuture()
			.get()
		provider.deferredResolutions[ServiceFile("1")]?.resolve()
		updatedNowPlayingAfterClearing = engine.clearPlaylist().toExpiringFuture()[1, TimeUnit.SECONDS]
		playlistAfterClearing = updatedNowPlayingAfterClearing?.playlist?.toList()
		isPlayingAfterPlaylistCleared = engine.isPlaying
		engine.addFile(ServiceFile("701")).toExpiringFuture().get()
		engine.resume().toExpiringFuture().get()
		provider.deferredResolutions[ServiceFile("701")]?.resolve()
	}

	@Test
	fun `then playback completion is signaled`() {
		assertThat(isPlaybackCompletionSignaled).isTrue
	}

	@Test
	fun `then the playlist is updated`() {
		assertThat(playlistAfterClearing).isEmpty()
	}

	@Test
	fun `then the playing file is correct after clearing`() {
		assertThat(updatedNowPlayingAfterClearing?.playlistPosition).isEqualTo(0)
	}

	@Test
	fun `then the file position is correct after clearing`() {
		assertThat(updatedNowPlayingAfterClearing?.filePosition).isEqualTo(0)
	}

	@Test
	fun `then it is not playing after the playlist is cleared`() {
		assertThat(isPlayingAfterPlaylistCleared).isFalse
	}

	@Test
	fun `then playback is resumed`() {
		assertThat(mut.second.isPlaying).isTrue
	}

	@Test
	fun `then the playing files is correct`() {
		assertThat(playingFiles).containsExactly(ServiceFile("1"), ServiceFile("701"))
	}
}
