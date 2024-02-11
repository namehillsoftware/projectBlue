package com.lasthopesoftware.bluewater.client.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.ObservableEmitter
import org.joda.time.Duration
import java.io.Closeable
import java.io.IOException

private val logger by lazyLogger<PlaylistPlayer>()

class PlaylistPlayer(private val preparedPlaybackFileProvider: SupplyQueuedPreparedFiles, private val preparedPosition: Duration) : IPlaylistPlayer, Closeable {

	private val stateChangeSync = Any()
	private var positionedPlayingFile: PositionedPlayingFile? = null
	private var positionedPlayableFile: PositionedPlayableFile? = null
	private var volume = 0f

	private var lastStateChangePromise: Promise<*> = Promise.empty<Any>()

	@Volatile
	private var isStarted = false
	private var emitter: ObservableEmitter<PositionedPlayingFile>? = null

	override fun subscribe(e: ObservableEmitter<PositionedPlayingFile>) {
		emitter = e
		if (!isStarted) {
			isStarted = true
			setupNextPreparedFile(preparedPosition)
		}
	}

	override fun pause(): Promise<PositionedPlayableFile?> {
		synchronized(stateChangeSync) {
			return lastStateChangePromise
				.eventually({ promisePause() }, { promisePause() })
				.also { lastStateChangePromise = it }
		}
	}

	override fun resume(): Promise<PositionedPlayingFile?> {
		synchronized(stateChangeSync) {
			return lastStateChangePromise
				.eventually({ promiseResumption() }, { promiseResumption() })
				.also { lastStateChangePromise = it }
		}
	}

	override val isPlaying: Boolean
		get() = positionedPlayingFile != null

	override fun setVolume(volume: Float): Promise<Unit> {
		this.volume = volume
		return positionedPlayableFile?.playableFileVolumeManager?.setVolume(volume)?.unitResponse() ?: Unit.toPromise()
	}

	override fun haltPlayback(): Promise<*> {
		fun generateHaltPromise() = positionedPlayableFile?.playableFile?.toPromise()
				?: positionedPlayingFile?.playingFile?.promisePause()
				?: Promise.empty()

		return synchronized(stateChangeSync) {
			lastStateChangePromise
				.eventually(
					{ generateHaltPromise() },
					{ generateHaltPromise() })
				.then({ p ->
					try {
						p?.close()
					} catch (e: Throwable) {
						logger.error("There was an error releasing the media player", e)
						emitter?.onError(e)
					}
				}, { e ->
					logger.error("There was an error releasing the media player", e)
					emitter?.onError(e)
				})
				.also { lastStateChangePromise = it }
		}
	}

	private fun promisePause(): Promise<PositionedPlayableFile?> {
		return positionedPlayingFile
			?.let {
				it.playingFile
					.promisePause()
					.then { p ->
						PositionedPlayableFile(p, it.playableFileVolumeManager,	it.asPositionedFile()).apply {
							positionedPlayableFile = this
							positionedPlayingFile = null
						}
					}
			}
			?: positionedPlayableFile.toPromise()
	}

	private fun promiseResumption(): Promise<PositionedPlayingFile?> {
		return positionedPlayableFile
			?.let {
				it.playableFile
					.promisePlayback()
					.then { p ->
						PositionedPlayingFile(p, it.playableFileVolumeManager, it.asPositionedFile()).apply {
							positionedPlayingFile = this
							positionedPlayableFile = null
						}
					}
			}
			?: Promise(IllegalStateException("A file must not be playing in order to resume"))
	}

	private fun setupNextPreparedFile(preparedPosition: Duration = Duration.ZERO) {
		val preparingPlaybackFile = preparedPlaybackFileProvider
			.promiseNextPreparedPlaybackFile(preparedPosition)

		if (preparingPlaybackFile == null) {
			doCompletion()
			return
		}

		preparingPlaybackFile
			.eventually { positionedPlayableFile -> startFilePlayback(positionedPlayableFile) }
			.excuse { exception -> handlePlaybackException(exception) }
	}

	private fun startFilePlayback(positionedPlayableFile: PositionedPlayableFile): Promise<PlayingFile?> {
		positionedPlayableFile.playableFileVolumeManager.setVolume(volume)
		val playbackHandler = positionedPlayableFile.playableFile

		synchronized(stateChangeSync) {
			this.positionedPlayableFile = positionedPlayableFile
			val promisedPlayback = lastStateChangePromise.eventually(
				{ playbackHandler.promisePlayback() },
				{ playbackHandler.promisePlayback() })

			lastStateChangePromise = promisedPlayback
				.then { playingFile ->
					val newPositionedPlayingFile = PositionedPlayingFile(
						playingFile,
						positionedPlayableFile.playableFileVolumeManager,
						positionedPlayableFile.asPositionedFile())

					positionedPlayingFile = newPositionedPlayingFile

					emitter?.onNext(newPositionedPlayingFile)

					playingFile
						.promisePlayedFile()
						.then(
							{ closeAndStartNextFile(playbackHandler) },
							{ handlePlaybackException(it) })
				}
			return promisedPlayback
		}
	}

	private fun closeAndStartNextFile(playbackHandler: PlayableFile) {
		try {
			playbackHandler.close()
		} catch (e: IOException) {
			logger.error("There was an error releasing the media player", e)
		}
		setupNextPreparedFile()
	}

	private fun handlePlaybackException(exception: Throwable) {
		emitter?.onError(exception)
		haltPlayback()
	}

	override fun close() {
		haltPlayback()
	}

	private fun doCompletion() {
		positionedPlayingFile = null
		emitter?.onComplete()
	}
}
