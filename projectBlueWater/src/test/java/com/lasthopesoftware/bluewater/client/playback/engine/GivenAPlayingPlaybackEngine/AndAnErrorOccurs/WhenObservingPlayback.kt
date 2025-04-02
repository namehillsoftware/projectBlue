package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndAnErrorOccurs

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 617

class WhenObservingPlayback {

	private val mut by lazy {
		val deferredErrorPlaybackPreparer = DeferredErrorPlaybackPreparer()
		val fakePlaybackPreparerProvider: IPlayableFilePreparationSourceProvider =
			object : IPlayableFilePreparationSourceProvider {
				override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource =
					deferredErrorPlaybackPreparer

                override val maxQueueSize: Int
                    get() = 1
			}
		val library = Library(id = libraryId)
		val libraryProvider = FakeLibraryRepository(library)
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				fakePlaybackPreparerProvider
			),
			listOf(CompletingFileQueueProvider()),
			NowPlayingRepository(
				FakeSelectedLibraryProvider(),
				libraryProvider,
				libraryProvider,
				FakeNowPlayingState(),
			),
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Pair(deferredErrorPlaybackPreparer, playbackEngine)
	}

	private var error: Throwable? = null

	@BeforeAll
	fun act() {
		val (deferredErrorPlaybackPreparer, playbackEngine) = mut

		val promisedStart = playbackEngine
			.setOnPlaylistError { e -> error = e }
			.startPlaylist(
				LibraryId(libraryId),
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				),
				0,
				Duration.ZERO
			)
		deferredErrorPlaybackPreparer.resolve()
		promisedStart.toExpiringFuture().get()
	}

	@Test
	fun `then the error is broadcast`() {
		assertThat(error).isNotNull
	}

	private class DeferredErrorPlaybackPreparer : Promise<PreparedPlayableFile?>(), PlayableFilePreparationSource {
		fun resolve() {
			reject(Exception())
		}

		override fun promisePreparedPlaybackFile(libraryId: LibraryId, serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile?> = this
	}
}
