package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap

import androidx.lifecycle.AtomicReference
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ManagePlaybackQueues
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.ProvidePositionedFileQueue
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.playlist.ManagePlaylistPlayback
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.io.Closeable

class ManagedPlaylistPlayer(
	private val volumeManagement: PlaylistVolumeManager,
	private val playbackQueues: ManagePlaybackQueues,
	private val nowPlayingState: GetNowPlayingState,
	positionedFileQueueProviders: Iterable<ProvidePositionedFileQueue>,
) : BootstrapPlayback, ManagePlaylistPlayback, Closeable {

	private val playerSync = Any()
	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	private val promisedNowPlayState = AtomicReference(Pair<NowPlaying?, NowPlaying?>(null, null).toPromise())

	@Volatile
	private var playlistPlayer: PlaylistPlayer? = null

	override fun updateFromState(libraryId: LibraryId): Promise<NowPlaying?> =
		promisedNowPlayState
			.updateAndGet { promisedState ->
				promisedState
					.eventually { (_, op) ->
						nowPlayingState
							.promiseNowPlaying(libraryId)
							.then { nowPlaying ->
								Pair(op, nowPlaying)
							}
					}
			}
			.then { (op, np) ->
				// Return new np, but create playlist player as a side-effect
				np?.apply {
					if (this != op) {
						synchronized(playerSync) {
							playlistPlayer?.close()
							val positionedFileQueueProvider = positionedFileQueueProviders.getValue(isRepeating)
							val queue =
								positionedFileQueueProvider.provideQueue(libraryId, playlist, playlistPosition)
							val preparedPlaybackQueue = playbackQueues.initializePreparedPlaybackQueue(queue)
							val newPlayer = PlaylistPlayer(preparedPlaybackQueue, Duration.millis(filePosition))

							volumeManagement.managePlayer(newPlayer)
							playlistPlayer = newPlayer
						}
					}
				}
			}

	override fun close(): Unit = synchronized(playerSync)  {
		playlistPlayer?.close()
		playlistPlayer = null
	}

	override fun pause(): Promise<PositionedPlayableFile?> = playlistPlayer?.pause().keepPromise()

	override fun resume(): Promise<PositionedPlayingFile?> = playlistPlayer?.resume().keepPromise()

	override fun setVolume(volume: Float): Promise<Unit> = playlistPlayer?.setVolume(volume).keepPromise(Unit)

	override fun haltPlayback(): Promise<Unit> = playlistPlayer
		?.haltPlayback()
		?.must { _ -> synchronized(playerSync) { playlistPlayer = null } }
		.keepPromise(Unit)

	override fun promisePlayedPlaylist(): ProgressingPromise<PositionedPlayingFile, Unit> = playlistPlayer
		?.promisePlayedPlaylist()
		?: ProgressingPromise(Unit)

	override val isPlaying: Boolean
		get() = playlistPlayer?.isPlaying ?: false

}
