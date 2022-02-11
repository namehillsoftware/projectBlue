package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine.AndAnErrorOccurs

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenObservingPlayback {
	@Test
	fun thenTheErrorIsBroadcast() {
		assertThat(error).isNotNull
	}

	private class DeferredErrorPlaybackPreparer : PlayableFilePreparationSource {
		private var reject: Messenger<PreparedPlayableFile>? = null
		fun resolve() {
			if (reject != null) reject!!.sendRejection(Exception())
		}

		override fun promisePreparedPlaybackFile(
			serviceFile: ServiceFile,
			preparedAt: Duration
		): Promise<PreparedPlayableFile> {
			return Promise { messenger: Messenger<PreparedPlayableFile>? -> reject = messenger }
		}
	}

	companion object {
		private var error: Throwable? = null

		@BeforeClass
		@JvmStatic
		fun context() {
			val deferredErrorPlaybackPreparer = DeferredErrorPlaybackPreparer()
			val fakePlaybackPreparerProvider: IPlayableFilePreparationSourceProvider =
				object : IPlayableFilePreparationSourceProvider {
					override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource {
						return deferredErrorPlaybackPreparer
					}

					override fun getMaxQueueSize(): Int {
						return 1
					}
				}
			val library = Library()
			library.setId(1)
			val libraryProvider = PassThroughSpecificLibraryProvider(library)
			val libraryStorage = PassThroughLibraryStorage()
			val playbackEngine = PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(
					fakePlaybackPreparerProvider,
					fakePlaybackPreparerProvider
				), listOf(CompletingFileQueueProvider()),
                NowPlayingRepository(
                    libraryProvider,
                    libraryStorage
                ),
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)

			playbackEngine
				.setOnPlaylistError { e: Throwable? -> error = e }
				.startPlaylist(
					listOf(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(3),
						ServiceFile(4),
						ServiceFile(5)
					), 0, Duration.ZERO
				)
			deferredErrorPlaybackPreparer.resolve()
		}
	}
}
