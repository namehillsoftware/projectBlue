package com.lasthopesoftware.bluewater.client.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.ForwardedResponse.Companion.thenForward
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

class PlaylistPlayer(
	private val preparedPlaybackFileProvider: SupplyQueuedPreparedFiles,
	private val preparedPosition: Duration
) :
	ManagePlaylistPlayback, PromisingCloseable, ProgressingPromise<PositionedPlayingFile, Unit>()
{
	companion object {
		private val logger by lazyLogger<PlaylistPlayer>()
		private const val releasingMediaPlayerError = "There was an error releasing the media player"
	}

	private val stateChangeSync = Any()

	@Volatile
	private var positionedPlayingFile: Promise<PositionedPlayingFile?>? = null

	@Volatile
	private var positionedPlayableFile: Promise<PositionedPlayableFile?>? = null

	@Volatile
	private var volume = 0f

	private val isHalted = AtomicBoolean()

	override fun promisePlayedPlaylist(): ProgressingPromise<PositionedPlayingFile, Unit> = this

	override fun pause(): Promise<PositionedPlayableFile?> = synchronized(stateChangeSync) {
		positionedPlayingFile
			?.run {
				cancel()
				eventually { ppf ->
					ppf
						?.playingFile
						?.promisePause()
						?.then { p ->
							PositionedPlayableFile(p, ppf.playableFileVolumeManager, ppf.asPositionedFile())
						}
						?: positionedPlayableFile.keepPromise()
				}
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

	override fun setVolume(volume: Float): Promise<Unit> = synchronized(stateChangeSync) {
		this.volume = volume
		return positionedPlayableFile
			?.eventually { it?.playableFileVolumeManager?.setVolume(volume)?.unitResponse().keepPromise(Unit) }
			?: positionedPlayingFile?.eventually {
				it?.playableFileVolumeManager?.setVolume(volume)?.unitResponse().keepPromise(Unit)
			}.keepPromise(Unit)
	}

	override fun promiseClose(): Promise<Unit> = haltPlayback()

	override fun haltPlayback(): Promise<Unit> {
		val wasHalted = isHalted.getAndSet(true)
		return synchronized(stateChangeSync) {
			positionedPlayingFile
				?.run {
					cancel()
					eventually { ppf ->
						ppf
							?.playingFile
							?.promisePause()
							?.then { p ->
								PositionedPlayableFile(p, ppf.playableFileVolumeManager, ppf.asPositionedFile())
							}
							.keepPromise()
					}
				}
				.also {
					positionedPlayableFile?.then { f -> f?.playableFile?.close() }
					positionedPlayableFile = null
					positionedPlayingFile = null
				}
		}
			.keepPromise()
			.then({ p ->
				if (!wasHalted) {
					try {
						p?.playableFile?.close()
					} catch (e: Throwable) {
						logger.error(releasingMediaPlayerError, e)
						reject(e)
						throw e
					}
				}
			}, { e ->
				if (e is CancellationException) return@then
				if (e is PreparationException && e.cause is CancellationException) return@then

				logger.error(releasingMediaPlayerError, e)
				reject(e)
				throw e
			})
			.then { _ -> reject(CancellationException("PlaylistPlayer halted.")) }
	}

	private fun playNextFile(preparedPosition: Duration = Duration.ZERO): Promise<PositionedPlayingFile?> {
		fun closeAndStartNextFile(playbackHandler: PlayableFile) = synchronized(stateChangeSync) {
			try {
				playbackHandler.close()
			} catch (e: IOException) {
				logger.error(releasingMediaPlayerError, e)
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

		return if (!isHalted.get()) object : Promise<PositionedPlayingFile?>(), CancellationResponse {
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
								if (!isCancelled && !isHalted.get()) {
									isStarting = true
									startFilePlayback(pf).then(::resolve, ::reject)
								} else {
									resolve(null)
								}
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
		} else empty()
	}

	private fun handlePlaybackException(exception: Throwable) {
		if (exception is CancellationException || exception.cause is CancellationException) return

		reject(exception)
		haltPlayback()
	}

	private fun doCompletion() {
		positionedPlayingFile = null
		resolve(Unit)
	}
}
