package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPreparingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPlaybackIsInterrupted {

	companion object {
		private const val libraryId = 480
	}

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
			listOf(
				ServiceFile("1184"),
				ServiceFile("591"),
				ServiceFile("1093"),
				ServiceFile("447"),
				ServiceFile("5331")
			)
		)

		val deferredNowPlaying = DeferredPromise(nowPlaying)

		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			mockk {
				every { promiseNowPlaying(LibraryId(libraryId)) } answers {
					nowPlaying.toPromise()
				}

			},
			listOf(CompletingFileQueueProvider()),
		)
		val playbackEngine = PlaybackEngine(
			preparedPlaybackQueueResourceManagement,
			listOf(CompletingFileQueueProvider()),
			mockk {
				every { promiseNowPlaying(LibraryId(libraryId)) } answers {
					nowPlaying.toPromise()
				}

				every { updateNowPlaying(any()) } answers {
					nowPlaying = firstArg()
					deferredNowPlaying
				}
			},
			playbackBootstrapper,
			playbackBootstrapper,
		)

		Triple(fakePlaybackPreparerProvider, deferredNowPlaying, playbackEngine)
	}

	private var isInterrupted = false
	private var nowPlaying = NowPlaying(
		LibraryId(libraryId),
		emptyList(),
		0,
		0,
		false
	)

	@BeforeAll
	fun before() {
		val (fakePlaybackPreparerProvider, deferredNowPlaying, playbackEngine) = mut

		playbackEngine.setOnPlaybackInterrupted { isInterrupted = true }

		playbackEngine
			.startPlaylist(
				LibraryId(libraryId),
				fakePlaybackPreparerProvider.deferredResolutions.keys.toList(),
				0
			)

		playbackEngine.interrupt().also {
			deferredNowPlaying.resolve()
			fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1184")]?.resolve()
		}.toExpiringFuture().get()
	}

	@Test
	fun `then the playback state is not playing`() {
		assertThat(mut.third.isPlaying).isFalse
	}

	@Test
	fun `then the saved file position is correct`() {
		assertThat(nowPlaying.filePosition).isEqualTo(0)
	}

	@Test
	fun `then the saved playlist position is correct`() {
		assertThat(nowPlaying.playlistPosition).isEqualTo(0)
	}

	@Test
	fun `then playback is reported as interrupted`() {
		assertThat(isInterrupted).isTrue
	}

	@Test
	fun `then the saved playlist is correct`() {
		assertThat(nowPlaying.playlist).isEqualTo(
			listOf(
				ServiceFile("1184"),
				ServiceFile("591"),
				ServiceFile("1093"),
				ServiceFile("447"),
				ServiceFile("5331")
			)
		)
	}
}
