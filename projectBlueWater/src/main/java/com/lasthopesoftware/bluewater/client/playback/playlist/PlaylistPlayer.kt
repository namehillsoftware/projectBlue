package com.lasthopesoftware.bluewater.client.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.joda.time.Duration
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CancellationException

private val logger by lazyLogger<PlaylistPlayer>()

class PlaylistPlayer(private val preparedPlaybackFileProvider: SupplyQueuedPreparedFiles, private val preparedPosition: Duration) :
	ManagePlaylistPlayback, Closeable {

	private val behaviorSubject = BehaviorSubject.create<PositionedPlayingFile>()
	private val stateChangeSync = Any()

	@Volatile
	private var positionedPlayingFile: Promise<PositionedPlayingFile?>? = null

	@Volatile
	private var positionedPlayableFile: Promise<PositionedPlayableFile?>? = null

	@Volatile
	private var volume = 0f

	@Volatile
	private var promisedPlayableFile: Promise<PositionedPlayableFile> = Promise.empty()

	@Volatile
	private var isHalted = false

	override fun observe(): Observable<PositionedPlayingFile> = behaviorSubject

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
					}.keepPromise()
			}
			?.also {
				positionedPlayableFile = it
				positionedPlayingFile = null
			}
			?: positionedPlayableFile
			?: Promise.empty()
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
					positionedPlayableFile = null
				}
	}

	override val isPlaying: Boolean
		get() = positionedPlayingFile != null

	override fun setVolume(volume: Float): Promise<Unit> {
		this.volume = volume
		return positionedPlayableFile
			?.eventually { it?.playableFileVolumeManager?.setVolume(volume)?.unitResponse() ?: Unit.toPromise() }
			?: positionedPlayingFile?.eventually {
				it?.playableFileVolumeManager?.setVolume(volume)?.unitResponse() ?: Unit.toPromise()
			}
			?: Unit.toPromise()
	}

	override fun haltPlayback(): Promise<*> = synchronized(stateChangeSync) {
		pause()
			.then({ p ->
				if (!isHalted) {
					promisedPlayableFile.cancel()
					isHalted = true
					try {
						p?.playableFile?.close()
					} catch (e: Throwable) {
						logger.error("There was an error releasing the media player", e)
						behaviorSubject.onError(e)
					}
				}
			}, { e ->
				logger.error("There was an error releasing the media player", e)
				behaviorSubject.onError(e)
			})
	}

	private fun playNextFile(preparedPosition: Duration = Duration.ZERO): Promise<PositionedPlayingFile?> =
		object : Promise<PositionedPlayingFile?>(), Runnable {

			var isCancelled = false

			init {
				respondToCancellation(this)

				val preparedPlaybackFile = preparedPlaybackFileProvider.promiseNextPreparedPlaybackFile(preparedPosition)

				if (preparedPlaybackFile == null || isCancelled) {
					doCompletion()
					resolve(null)
				} else {
					promisedPlayableFile = preparedPlaybackFile
					preparedPlaybackFile.excuse(::handlePlaybackException)

					preparedPlaybackFile.then {
						if (!isCancelled) startFilePlayback(it).then(::resolve, ::reject)
						else resolve(null)
					}
				}
			}

			override fun run() {
				isCancelled = true
				resolve(null)
			}
		}

	private fun startFilePlayback(positionedPlayableFile: PositionedPlayableFile): Promise<PositionedPlayingFile> {
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

				behaviorSubject.onNext(newPositionedPlayingFile)

				playingFile
					.promisePlayedFile()
					.then(
						{ closeAndStartNextFile(playbackHandler) },
						{ handlePlaybackException(it) })

				newPositionedPlayingFile
			}
	}

	private fun closeAndStartNextFile(playbackHandler: PlayableFile) = synchronized(stateChangeSync) {
		try {
			playbackHandler.close()
		} catch (e: IOException) {
			logger.error("There was an error releasing the media player", e)
		}
		positionedPlayingFile = playNextFile()
	}

	private fun handlePlaybackException(exception: Throwable) {
		if (isHalted && exception is CancellationException) return

		behaviorSubject.onError(exception)
		haltPlayback()
	}

	override fun close() {
		haltPlayback()
	}

	private fun doCompletion() {
		positionedPlayingFile = null
		behaviorSubject.onComplete()
	}
}
