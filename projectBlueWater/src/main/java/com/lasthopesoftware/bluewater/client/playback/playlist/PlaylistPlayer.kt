package com.lasthopesoftware.bluewater.client.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.ForwardedResponse.Companion.thenForward
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CancellationException

private val logger by lazyLogger<PlaylistPlayer>()

class PlaylistPlayer(private val preparedPlaybackFileProvider: SupplyQueuedPreparedFiles, private val preparedPosition: Duration) :
	ManagePlaylistPlayback, Closeable, ProgressingPromise<PositionedPlayingFile, Unit>() {

	private val stateChangeSync = Any()

	@Volatile
	private var positionedPlayingFile: Promise<PositionedPlayingFile?>? = null

	@Volatile
	private var positionedPlayableFile: Promise<PositionedPlayableFile?>? = null

	@Volatile
	private var volume = 0f

	@Volatile
	private var isHalted = false

	override fun promisePlayedPlaylist(): ProgressingPromise<PositionedPlayingFile, Unit> = this

	override fun pause(): Promise<PositionedPlayableFile?> = synchronized(stateChangeSync) {
		positionedPlayingFile
			?.apply { cancel() }
			?.eventually { ppf ->
				ppf
					?.let { playingFile ->
						playingFile
							.playingFile
							.promisePause()
							.then { p ->
								PositionedPlayableFile(p, ppf.playableFileVolumeManager, ppf.asPositionedFile())
							}
					}
					?: positionedPlayableFile.keepPromise()
			}
			?.also {
				positionedPlayableFile = it
				positionedPlayingFile = null
			}
			?: positionedPlayableFile
			?: empty()
	}

	override fun resume(): Promise<PositionedPlayingFile?> = synchronized(stateChangeSync) {
		positionedPlayingFile
			?: (positionedPlayableFile
				?.eventually {
					it?.playableFile?.let { playableFile ->
						playableFile
							.promisePlayback()
							.then { p ->
								PositionedPlayingFile(p, it.playableFileVolumeManager, it.asPositionedFile())
							}
					}.keepPromise()
				}
				?: playNextFile(preparedPosition))
				.also {
					positionedPlayingFile = it
				}
	}

	override val isPlaying: Boolean
		get() = positionedPlayingFile != null

	override fun setVolume(volume: Float): Promise<Unit> {
		this.volume = volume
		return positionedPlayableFile
			?.eventually { it?.playableFileVolumeManager?.setVolume(volume)?.unitResponse().keepPromise(Unit) }
			?: positionedPlayingFile?.eventually {
				it?.playableFileVolumeManager?.setVolume(volume)?.unitResponse().keepPromise(Unit)
			}.keepPromise(Unit)
	}

	override fun haltPlayback(): Promise<*> = synchronized(stateChangeSync) {
		pause()
			.then({ p ->
				if (!isHalted) {
					isHalted = true
					try {
						p?.playableFile?.close()
					} catch (e: Throwable) {
						logger.error("There was an error releasing the media player", e)
						reject(e)
						throw e
					}
				}
			}, { e ->
				isHalted = true

				logger.error("There was an error releasing the media player", e)
				reject(e)
				throw e
			})
	}

	private fun playNextFile(preparedPosition: Duration = Duration.ZERO): Promise<PositionedPlayingFile?> {
		fun closeAndStartNextFile(playbackHandler: PlayableFile) = synchronized(stateChangeSync) {
			try {
				playbackHandler.close()
			} catch (e: IOException) {
				logger.error("There was an error releasing the media player", e)
			}
			positionedPlayingFile = playNextFile()
		}

		fun startFilePlayback(positionedPlayableFile: PositionedPlayableFile): Promise<PositionedPlayingFile> {
			positionedPlayableFile.playableFileVolumeManager.setVolume(volume)
			val playbackHandler = positionedPlayableFile.playableFile

			return playbackHandler
				.promisePlayback()
				.then { playingFile ->
					val newPositionedPlayingFile = PositionedPlayingFile(
						playingFile,
						positionedPlayableFile.playableFileVolumeManager,
						positionedPlayableFile.asPositionedFile()
					)

					reportProgress(newPositionedPlayingFile)

					playingFile
						.promisePlayedFile()
						.then(
							{ closeAndStartNextFile(playbackHandler) },
							::handlePlaybackException
						)

					newPositionedPlayingFile
				}
		}

		return object : Promise<PositionedPlayingFile?>(), CancellationResponse {

			private val sync = Any()

			@Volatile
			private var isCancelled = false

			@Volatile
			private var isStarting = false

			init {
				awaitCancellation(this)

				val preparedPlaybackFile = preparedPlaybackFileProvider.promiseNextPreparedPlaybackFile(preparedPosition)
				positionedPlayableFile = preparedPlaybackFile?.thenForward()

				if (preparedPlaybackFile == null) {
					doCompletion()
					resolve(null)
				} else {
					preparedPlaybackFile
						.then({ pf ->
							synchronized(sync) {
								if (!isCancelled) {
									isStarting = true
									startFilePlayback(pf).then(::resolve, ::reject)
								} else resolve(null)
							}
						}, { e ->
							handlePlaybackException(e)
							reject(e)
						})
				}
			}

			override fun cancellationRequested() = synchronized(sync) {
				isCancelled = true

				if (!isStarting)
					resolve(null)
			}
		}
	}

	private fun handlePlaybackException(exception: Throwable) {
		if (exception is CancellationException || exception.cause is CancellationException) return

		reject(exception)
		haltPlayback()
	}

	override fun close() {
		haltPlayback()
	}

	private fun doCompletion() {
		positionedPlayingFile = null
		resolve(Unit)
	}
}
