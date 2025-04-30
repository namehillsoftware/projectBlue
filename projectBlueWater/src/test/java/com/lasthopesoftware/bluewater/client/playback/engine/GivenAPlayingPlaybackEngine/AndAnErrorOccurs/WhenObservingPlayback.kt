package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndAnErrorOccurs

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
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
		val fakePlaybackPreparerProvider = object : IPlayableFilePreparationSourceProvider {
			override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource =
				deferredErrorPlaybackPreparer

			override val maxQueueSize: Int
				get() = 1
		}
		val library = Library(id = libraryId)
		val libraryProvider = FakeLibraryRepository(library)
		val preparedPlaybackQueueResourceManagement = PreparedPlaybackQueueResourceManagement(
			fakePlaybackPreparerProvider,
			fakePlaybackPreparerProvider
		)
		val repository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			libraryProvider,
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			repository,
			listOf(CompletingFileQueueProvider()),
		)
		val playbackEngine = PlaybackEngine(
			preparedPlaybackQueueResourceManagement,
			listOf(CompletingFileQueueProvider()),
			repository,
			playbackBootstrapper,
			playbackBootstrapper,
		)

		Pair(deferredErrorPlaybackPreparer, playbackEngine)
	}

	private var error: Throwable? = null

	@BeforeAll
	fun act() {
		val (deferredErrorPlaybackPreparer, playbackEngine) = mut

		val promisedStart = playbackEngine
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

		val promisedError = Promise {
			playbackEngine
				.setOnPlaylistError { e ->
					it.sendResolution(e)
				}
		}

		deferredErrorPlaybackPreparer.resolve()
		promisedStart.toExpiringFuture().get()
		error = promisedError.toExpiringFuture().get()
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
