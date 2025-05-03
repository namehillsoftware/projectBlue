package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPreparingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.ResolvablePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Collections

class WhenChangingTracks {

	companion object {
		private const val libraryId = 265
	}

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
			listOf(
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("3"),
				ServiceFile("4"),
				ServiceFile("5")
			)
		)

		val deferredNowPlaying = ResolvablePromise<NowPlaying>()
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
		val playbackEngine =
			PlaybackEngine(
				preparedPlaybackQueueResourceManagement,
				listOf(CompletingFileQueueProvider()),
				mockk {
					every { promiseNowPlaying(LibraryId(libraryId)) } answers {
						nowPlaying.toPromise()
					}

					every { updateNowPlaying(any()) } answers {
						val returnNowPlaying = firstArg<NowPlaying>()
						nowPlaying = returnNowPlaying

						if (!deferredNowPlaying.isResolved) deferredNowPlaying
						else returnNowPlaying.toPromise()
					}
				},
				playbackBootstrapper,
				playbackBootstrapper,
			)
		Triple(fakePlaybackPreparerProvider, deferredNowPlaying, playbackEngine)
	}

	private var nowPlaying = NowPlaying(
		LibraryId(libraryId),
		emptyList(),
		0,
		0,
		false
	)

	private var nextSwitchedFile: PositionedFile? = null
	private var latestFile: PositionedPlayingFile? = null
	private var firstGuy: ResolvablePlaybackHandler? = null
	private var secondGuy: ResolvablePlaybackHandler? = null
	private val startedFiles = Collections.synchronizedList(ArrayList<PositionedPlayingFile?>())

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, deferredNowPlaying, playbackEngine) = mut

		val promisedStart = playbackEngine
			.setOnPlayingFileChanged { _, p ->
				startedFiles.add(p)
				latestFile = p
			}
			.startPlaylist(
				LibraryId(libraryId),
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				),
				0
			)

		val promisedPositionChange = playbackEngine.changePosition(2, Duration.ZERO)

		deferredNowPlaying.sendResolution(nowPlaying)

		firstGuy = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()
		secondGuy = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("3")]?.resolve()

		promisedStart.toExpiringFuture().get()
		nextSwitchedFile = promisedPositionChange.toExpiringFuture().get()?.second
	}

	@Test
	fun `then the playback state is playing`() {
		assertThat(mut.third.isPlaying).isTrue
	}

	@Test
	fun `then the first guy is not playing`() {
		assertThat(firstGuy?.isPlaying).isFalse
	}

	@Test
	fun `then the second guy is playing`() {
		assertThat(secondGuy?.isPlaying).isTrue
	}

	@Test
	fun `then the next file change is the switched to the correct track position`() {
		assertThat(nextSwitchedFile?.playlistPosition).isEqualTo(2)
	}

	@Test
	fun `then the changed started file is correct`() {
		assertThat(startedFiles[0]?.asPositionedFile())
			.isEqualTo(PositionedFile(2, ServiceFile("3")))
	}

	@Test
	fun `then the playlist is started once`() {
		assertThat(startedFiles).hasSize(1)
	}

	@Test
	fun `then now playing is correct`() {
		assertThat(nowPlaying).isEqualTo(
			NowPlaying(
				LibraryId(libraryId),
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				),
				2,
				0,
				false
			)
		)
	}
}
