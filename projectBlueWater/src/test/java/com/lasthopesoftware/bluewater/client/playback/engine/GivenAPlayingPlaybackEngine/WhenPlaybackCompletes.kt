package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPlaybackCompletes {

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library()
		library.setId(1)
		val libraryProvider = PassThroughSpecificLibraryProvider(library)
		val libraryStorage = PassThroughLibraryStorage()
		val nowPlayingRepository =
			NowPlayingRepository(
				libraryProvider,
				libraryStorage
			)
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider
			) { 1 }, listOf(CompletingFileQueueProvider()),
			nowPlayingRepository,
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Triple(fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine)
	}

	private var nowPlaying: NowPlaying? = null
	private var observedPlayingFile: PositionedPlayingFile? = null
	private var resetPositionedFile: PositionedFile? = null

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, nowPlayingRepository, playbackEngine) = mut
		playbackEngine
			.setOnPlayingFileChanged { f -> observedPlayingFile = f }
			.setOnPlaylistReset { f -> resetPositionedFile = f }
			.startPlaylist(
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(3),
					ServiceFile(4),
					ServiceFile(5)
				), 0, Duration.ZERO
			)
		var playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
		for (i in 0..3) {
			val newPlayingPlaybackHandler =
				fakePlaybackPreparerProvider.deferredResolution.resolve()
			playingPlaybackHandler.resolve()
			playingPlaybackHandler = newPlayingPlaybackHandler
		}
		playingPlaybackHandler.resolve()
		nowPlaying = nowPlayingRepository.promiseNowPlaying().toExpiringFuture().get()
	}

	@Test
	fun `then the playback state is not playing`() {
		assertThat(mut.third.isPlaying).isFalse
	}

	@Test
	fun `then the observed file position is correct`() {
		assertThat(observedPlayingFile!!.asPositionedFile())
			.isEqualTo(PositionedFile(4, ServiceFile(5)))
	}

	@Test
	fun `then the reset file position is zero`() {
		assertThat(resetPositionedFile).isEqualTo(PositionedFile(0, ServiceFile(1)))
	}

	@Test
	fun `then the saved file position is correct`() {
		assertThat(nowPlaying!!.filePosition).isEqualTo(0)
	}

	@Test
	fun `then the saved playlist position is correct`() {
		assertThat(nowPlaying!!.playlistPosition).isEqualTo(0)
	}

	@Test
	fun `then the saved playlist is correct`() {
		assertThat(nowPlaying!!.playlist)
			.containsExactly(
				ServiceFile(1),
				ServiceFile(2),
				ServiceFile(3),
				ServiceFile(4),
				ServiceFile(5)
			)
	}
}
