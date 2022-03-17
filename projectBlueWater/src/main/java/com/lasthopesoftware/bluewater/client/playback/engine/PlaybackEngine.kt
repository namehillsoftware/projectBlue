package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.IStartPlayback
import com.lasthopesoftware.bluewater.client.playback.engine.events.*
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ManagePlaybackQueues
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.disposables.Disposable
import io.reactivex.observables.ConnectableObservable
import org.jetbrains.annotations.Contract
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import kotlin.math.max

private val logger by lazy { LoggerFactory.getLogger(PlaybackEngine::class.java) }

@Contract(pure = true)
private fun getNextPosition(startingPosition: Int, playlist: Collection<ServiceFile>): Int =
	if (startingPosition < playlist.size - 1) startingPosition + 1 else 0

@Contract(pure = true)
private fun getPreviousPosition(startingPosition: Int): Int = max(startingPosition - 1, 0)

class PlaybackEngine(
    managePlaybackQueues: ManagePlaybackQueues,
    positionedFileQueueProviders: Iterable<IPositionedFileQueueProvider>,
    private val nowPlayingRepository: MaintainNowPlayingState,
    private val playbackBootstrapper: IStartPlayback,
) :
	ChangePlaybackState,
	ChangePlaybackStateForSystem,
	ChangePlaylistPosition,
	ChangePlaybackContinuity,
	ChangePlaylistFiles,
	RegisterPlaybackEngineEvents,
	AutoCloseable
{
	private val preparedPlaybackQueueResourceManagement = managePlaybackQueues
	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	var isPlaying = false
		private set

	private var playlist = mutableListOf<ServiceFile>()
	private var isRepeating = false
	private var playlistPosition = 0
	private var fileProgress: ReadFileProgress = StaticProgressedFile(Duration.ZERO.toPromise())

	private var playbackSubscription: Disposable? = null
	private var activePlayer: IActivePlayer? = null
	private var onPlayingFileChanged: OnPlayingFileChanged? = null
	private var onPlaylistError: OnPlaylistError? = null
	private var onPlaybackStarted: OnPlaybackStarted? = null
	private var onPlaybackPaused: OnPlaybackPaused? = null
	private var onPlaybackInterrupted: OnPlaybackInterrupted? = null
	private var onPlaybackCompleted: OnPlaybackCompleted? = null
	private var onPlaylistReset: OnPlaylistReset? = null

	override fun restoreFromSavedState(): Promise<PositionedProgressedFile?> =
		nowPlayingRepository.promiseNowPlaying()
			.then { np ->
				playlist = np?.playlist?.toMutableList() ?: mutableListOf()
				np?.playingFile
					?.let { positionedFile ->
						playlistPosition = positionedFile.playlistPosition
						val filePosition = Duration.millis(np.filePosition)
						fileProgress = StaticProgressedFile(filePosition.toPromise())
						PositionedProgressedFile(positionedFile.playlistPosition, positionedFile.serviceFile, filePosition)
					}
			}

	override fun startPlaylist(playlist: List<ServiceFile>, playlistPosition: Int, filePosition: Duration): Promise<Unit> {
		logger.info("Starting playback")
		this.playlist = playlist.toMutableList()
		this.playlistPosition = playlistPosition
		this.fileProgress = StaticProgressedFile(filePosition.toPromise())
		return saveState().then { resumePlayback() }
	}

	override fun skipToNext(): Promise<PositionedFile> {
		return changePosition(
			getNextPosition(playlistPosition, playlist),
			Duration.ZERO)
	}

	override fun skipToPrevious(): Promise<PositionedFile> {
		return changePosition(getPreviousPosition(playlistPosition), Duration.ZERO)
	}

	@Synchronized
	override fun changePosition(playlistPosition: Int, filePosition: Duration): Promise<PositionedFile> {
		playbackSubscription?.dispose()
		activePlayer = null

		this.playlistPosition = playlistPosition
		fileProgress = StaticProgressedFile(filePosition.toPromise())

		return with(saveState()) {
			if (!isPlaying) then {
				val serviceFile = playlist[playlistPosition]
				PositionedFile(playlistPosition, serviceFile)
			} else eventually {
				object : Promise<PositionedFile>() {
					init {
						val queueProvider = positionedFileQueueProviders.getValue(isRepeating)
						try {
							val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement
								.initializePreparedPlaybackQueue(queueProvider.provideQueue(playlist, playlistPosition))
							startPlayback(preparedPlaybackQueue, filePosition)
								.firstElement()
								.subscribe({ resolve(it.asPositionedFile()) }, { reject(it) })
						} catch (e: Exception) {
							reject(e)
						}
					}
				}
			}
		}
	}

	override fun playRepeatedly(): Promise<Unit> {
		isRepeating = true
		val promisedSave = saveState()
		updatePreparedFileQueueUsingState()
		return promisedSave.unitResponse()
	}

	override fun playToCompletion(): Promise<Unit> {
		isRepeating = false
		val promisedSave = saveState()
		updatePreparedFileQueueUsingState()
		return promisedSave.unitResponse()
	}

	override fun resume(): Promise<Unit> {
		return activePlayer
			?.also {
				isPlaying = true
				onPlaybackStarted?.onPlaybackStarted()
			}
			?.resume()
			?.then { onPlayingFileChanged?.onPlayingFileChanged(it) }
			?: resumePlayback()
	}

	override fun pause(): Promise<Unit> =
		pausePlayback().then { onPlaybackPaused?.onPlaybackPaused() }

	override fun interrupt(): Promise<Unit> =
		pausePlayback().then { onPlaybackInterrupted?.onPlaybackInterrupted() }

	override fun setOnPlayingFileChanged(onPlayingFileChanged: OnPlayingFileChanged?): PlaybackEngine {
		this.onPlayingFileChanged = onPlayingFileChanged
		return this
	}

	override fun setOnPlaylistError(onPlaylistError: OnPlaylistError?): PlaybackEngine {
		this.onPlaylistError = onPlaylistError
		return this
	}

	override fun setOnPlaybackStarted(onPlaybackStarted: OnPlaybackStarted?): PlaybackEngine {
		this.onPlaybackStarted = onPlaybackStarted
		return this
	}

	override fun setOnPlaybackPaused(onPlaybackPaused: OnPlaybackPaused?): PlaybackEngine {
		this.onPlaybackPaused = onPlaybackPaused
		return this
	}

	override fun setOnPlaybackInterrupted(onPlaybackInterrupted: OnPlaybackInterrupted?): PlaybackEngine {
		this.onPlaybackInterrupted = onPlaybackInterrupted
		return this
	}

	override fun setOnPlaybackCompleted(onPlaybackCompleted: OnPlaybackCompleted?): PlaybackEngine {
		this.onPlaybackCompleted = onPlaybackCompleted
		return this
	}

	override fun setOnPlaylistReset(onPlaylistReset: OnPlaylistReset?): PlaybackEngine {
		this.onPlaylistReset = onPlaylistReset
		return this
	}

	override fun addFile(serviceFile: ServiceFile): Promise<NowPlaying?> {
		playlist.add(serviceFile)
		updatePreparedFileQueueUsingState()
		return saveState()
	}

	override fun removeFileAtPosition(position: Int): Promise<NowPlaying?> {
		val promisedSkip = if (playlistPosition == position) {
			skipToNext()
		} else {
			Promise.empty()
		}

		return promisedSkip
			.eventually {
				playlist.removeAt(position)

				if (playlistPosition > position)
					playlistPosition -= 1

				updatePreparedFileQueueUsingState()
				saveState()
			}
	}

	override fun moveFile(position: Int, newPosition: Int): Promise<NowPlaying?> {
		if (position < 0 || newPosition < 0) return nowPlayingRepository.promiseNowPlaying()

		with (playlist) {
			if (position >= size || newPosition >= size) return nowPlayingRepository.promiseNowPlaying()
		}

		val removedFile = playlist.removeAt(position)
		playlist.add(newPosition, removedFile)

		playlistPosition = when (playlistPosition) {
			position -> newPosition
			in newPosition until position -> playlistPosition + 1
			in position..newPosition -> playlistPosition - 1
			else -> playlistPosition
		}

		updatePreparedFileQueueUsingState()
		return saveState()
	}

	private fun pausePlayback(): Promise<NowPlaying> {
		val promisedPause = activePlayer?.pause() ?: Unit.toPromise()

		isPlaying = false

		return promisedPause
			.eventually { saveState() }
	}

	private fun resumePlayback(): Promise<Unit> {
		val positionedFileQueueProvider = positionedFileQueueProviders.getValue(isRepeating)
		val fileQueue = positionedFileQueueProvider.provideQueue(playlist, playlistPosition)
		val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(fileQueue)
		return fileProgress.progress.then {	startPlayback(preparedPlaybackQueue, it) }
	}

	private fun startPlayback(preparedPlaybackQueue: PreparedPlayableFileQueue, filePosition: Duration): ConnectableObservable<PositionedPlayingFile> {
		playbackSubscription?.dispose()

		val newPlayer = playbackBootstrapper.startPlayback(preparedPlaybackQueue, filePosition)
		activePlayer = newPlayer

		isPlaying = true
		onPlaybackStarted?.onPlaybackStarted()

		val observable = newPlayer.observe()

		playbackSubscription = observable.subscribe(
			{ p ->
				isPlaying = true
				playlistPosition = p.playlistPosition
				fileProgress = ProgressingFile(p)
				saveState().then { onPlayingFileChanged?.onPlayingFileChanged(p) }
			},
			{ e ->
				if (e is PreparationException) {
					playlistPosition = e.positionedFile.playlistPosition
					fileProgress = StaticProgressedFile(Duration.ZERO.toPromise())
					saveState()
				}
				onPlaylistError?.onError(e)
			},
			{
				isPlaying = false
				activePlayer = null
				changePosition(0, Duration.ZERO)
					.then { positionedFile ->
						onPlaylistReset?.onPlaylistReset(positionedFile)
						onPlaybackCompleted?.onPlaybackCompleted()
					}
			})
		return observable
	}

	private fun updatePreparedFileQueueUsingState() {
		preparedPlaybackQueueResourceManagement.tryUpdateQueue(
			positionedFileQueueProviders.getValue(isRepeating).provideQueue(
				playlist,
				playlistPosition + 1))
	}

	private fun saveState(): Promise<NowPlaying?> {
		return nowPlayingRepository
			.promiseNowPlaying()
			.eventually { np ->
				np?.let {
					np.playlist = playlist
					np.playlistPosition = playlistPosition
					np.isRepeating = isRepeating
					fileProgress.progress.eventually {
						np.filePosition = it.millis
						nowPlayingRepository.updateNowPlaying(np)
					}
				}.keepPromise()
			}
	}

	override fun close() {
		isPlaying = false
		onPlaybackStarted = null
		onPlayingFileChanged = null
		onPlaylistError = null
		activePlayer = null
		playbackSubscription?.dispose()
	}

	private class StaticProgressedFile(override val progress: Promise<Duration>) : ReadFileProgress

	private class ProgressingFile(val positionedPlayingFile: PositionedPlayingFile)
		: ReadFileProgress {

		override val progress: Promise<Duration>
			get() = positionedPlayingFile.playingFile.promisePlayedFile().progress.then { it ?: Duration.ZERO }
	}
}
