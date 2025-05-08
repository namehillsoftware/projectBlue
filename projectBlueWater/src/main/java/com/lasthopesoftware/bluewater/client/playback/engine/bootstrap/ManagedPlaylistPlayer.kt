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
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
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

	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	private val promisedPlayer = AtomicReference(Promise(Pair<NowPlaying?, PlaylistPlayer?>(null, null)))

	@Volatile
	private var playlistPlayer: PlaylistPlayer? = null

	override fun updateFromState(libraryId: LibraryId): Promise<NowPlaying?> = promisedPlayer
		.updateAndGet { originallyPromised ->
			originallyPromised
				.eventually { (op, player) ->
					nowPlayingState
						.promiseNowPlaying(libraryId)
						.eventually { np ->
							when {
								op == np -> Pair(np, player).toPromise()
								np == null -> {
									playbackQueues.reset()
									player
										?.haltPlayback()
										.keepPromise()
										.then { _ -> Pair(null, null) }
								}
								else -> {
									with (np) {
										val positionedFileQueueProvider = positionedFileQueueProviders.getValue(isRepeating)
										val queue = positionedFileQueueProvider.provideQueue(libraryId, playlist, playlistPosition)
										val preparedPlaybackQueue = playbackQueues.initializePreparedPlaybackQueue(queue)

										player
											?.haltPlayback()
											.keepPromise()
											.then { _ ->
												val newPlayer = PlaylistPlayer(preparedPlaybackQueue, Duration.millis(filePosition))
												volumeManagement.managePlayer(newPlayer)
												Pair(np, newPlayer)
											}
									}
								}
							}
						}
				}
	}.then { (np, p) ->
		playlistPlayer = p
		np
	}

	override fun close() {
		promisedPlayer.get().then { (_, p) -> p?.close() }
	}

	override fun pause(): Promise<PositionedPlayableFile?> = promisedPlayer.get()
		.eventually { (_, p) -> p?.pause().keepPromise() }

	override fun resume(): Promise<PositionedPlayingFile?> = promisedPlayer.get()
		.eventually { (_, p) -> p?.resume().keepPromise() }

	override fun setVolume(volume: Float): Promise<Unit> = promisedPlayer.get()
		.eventually { (_, p) -> p?.setVolume(volume).keepPromise(Unit) }

	override fun haltPlayback(): Promise<Unit> = promisedPlayer.get()
		.eventually { (_, p) -> p?.haltPlayback().keepPromise(Unit) }

	override fun promisePlayedPlaylist(): ProgressingPromise<PositionedPlayingFile, Unit> =	object : ProgressingPromiseProxy<PositionedPlayingFile, Unit>() {
			init {
				promisedPlayer.get()
					.then { (_, p) ->
						val promisedPlaylist = p?.promisePlayedPlaylist()
						if (promisedPlaylist != null) proxy(promisedPlaylist)
						else resolve(Unit)
					}
			}
		}

	override val isPlaying: Boolean
		get() = playlistPlayer?.isPlaying ?: false
}
