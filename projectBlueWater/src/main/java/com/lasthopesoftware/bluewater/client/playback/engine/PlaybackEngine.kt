package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.IStartPlayback
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackCompleted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackInterrupted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackPaused
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackStarted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistError
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistReset
import com.lasthopesoftware.bluewater.client.playback.engine.events.RegisterPlaybackEngineEvents
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ManagePlaybackQueues
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.ProvidePositionedFileQueue
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

class PlaybackEngine(
	private val preparedPlaybackQueueResourceManagement: ManagePlaybackQueues,
	positionedFileQueueProviders: Iterable<ProvidePositionedFileQueue>,
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
	companion object {
		private val logger by lazy { LoggerFactory.getLogger(PlaybackEngine::class.java) }

		@Contract(pure = true)
		private fun getNextPosition(startingPosition: Int, playlist: Collection<ServiceFile>): Int =
			if (startingPosition < playlist.size - 1) startingPosition + 1 else 0

		@Contract(pure = true)
		private fun getPreviousPosition(startingPosition: Int): Int = max(startingPosition - 1, 0)

		private val defaultState by lazy {
			PlayingState(
				LibraryId(-1),
				mutableListOf(),
				false,
				0,
				StaticProgressedFile(Duration.ZERO.toPromise())
			)
		}
	}

	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	var isPlaying = false
		private set

	private val playingStateSync = Any()

	@Volatile
	private var promisedPlayingState = Pair(LibraryId(-1), Promise(defaultState))

	private var playbackSubscription: Disposable? = null
	private var activePlayer: IActivePlayer? = null
	private var onPlayingFileChanged: OnPlayingFileChanged? = null
	private var onPlaylistError: OnPlaylistError? = null
	private var onPlaybackStarted: OnPlaybackStarted? = null
	private var onPlaybackPaused: OnPlaybackPaused? = null
	private var onPlaybackInterrupted: OnPlaybackInterrupted? = null
	private var onPlaybackCompleted: OnPlaybackCompleted? = null
	private var onPlaylistReset: OnPlaylistReset? = null

	override fun restoreFromSavedState(libraryId: LibraryId): Promise<Pair<LibraryId, PositionedProgressedFile?>> {
		fun promiseState() = withState {
			playlist
				.getOrNull(playlistPosition)
				?.let { serviceFile ->
					fileProgress
						.progress
						.then { p ->
							Pair(libraryId, PositionedProgressedFile(playlistPosition, serviceFile, p))
						}
				}
				?: Pair(libraryId, null).toPromise()
		}

		val currentActiveLibraryId = promisedPlayingState.first

		if (libraryId == currentActiveLibraryId) {
			return promiseState()
		}

		return updateLibraryState(libraryId) { l ->
			pausePlayback()
				.eventually {
					nowPlayingRepository.promiseNowPlaying(l)
				}
				.then { np ->
					np?.run {
						PlayingState(
							libraryId,
							playlist.toMutableList(),
							isRepeating,
							playlistPosition,
							StaticProgressedFile(Duration.millis(filePosition).toPromise())
						)
					}
				}
		}.eventually {
			promiseState()
		}
	}

	override fun startPlaylist(libraryId: LibraryId, playlist: List<ServiceFile>, playlistPosition: Int, filePosition: Duration): Promise<Unit> {
		logger.info("Starting playback")
		return updateLibraryState(libraryId) {
			PlayingState(
				it,
				playlist.toMutableList(),
				isRepeating,
				playlistPosition,
				StaticProgressedFile(filePosition.toPromise())
			).toPromise()
		}.eventually {saveState() }.eventually { resumePlayback() }
	}

	override fun skipToNext(): Promise<Pair<LibraryId, PositionedFile>> = withState {
		changePosition(
			getNextPosition(playlistPosition, playlist),
			Duration.ZERO
		)
	}

	override fun skipToPrevious(): Promise<Pair<LibraryId, PositionedFile>> = withState {
		changePosition(
			getPreviousPosition(playlistPosition),
			Duration.ZERO
		)
	}

	override fun changePosition(playlistPosition: Int, filePosition: Duration): Promise<Pair<LibraryId, PositionedFile>> {
		playbackSubscription?.dispose()
		activePlayer = null

		return withState {
			this.playlistPosition = playlistPosition
			fileProgress = StaticProgressedFile(filePosition.toPromise())

			with(saveState()) {
				if (!isPlaying) then {
					val serviceFile = playlist[playlistPosition]
					Pair(activeLibraryId, PositionedFile(playlistPosition, serviceFile))
				} else eventually {
					object : Promise<Pair<LibraryId, PositionedFile>>() {
						init {
							val queueProvider = positionedFileQueueProviders.getValue(isRepeating)
							try {
								val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement
									.initializePreparedPlaybackQueue(
										queueProvider.provideQueue(
											activeLibraryId,
											playlist,
											playlistPosition
										)
									)

								startPlayback(preparedPlaybackQueue, filePosition)
									.firstElement()
									.subscribe(
										{ resolve(Pair(activeLibraryId, it.asPositionedFile())) },
										{ reject(it) })
							} catch (e: Exception) {
								reject(e)
							}
						}
					}
				}
			}
		}
	}

	override fun playRepeatedly(): Promise<Unit> = withState {
		isRepeating = true
		val promisedSave = saveState()
		updatePreparedFileQueueUsingState()
		promisedSave
	}.unitResponse()

	override fun playToCompletion(): Promise<Unit> = withState {
		isRepeating = false
		val promisedSave = saveState()
		updatePreparedFileQueueUsingState()
		promisedSave
	}.unitResponse()

	override fun resume(): Promise<Unit> {
		return activePlayer
			?.also {
				isPlaying = true
				onPlaybackStarted?.onPlaybackStarted()
			}
			?.resume()
			?.then { onPlayingFileChanged?.onPlayingFileChanged(promisedPlayingState.first, it) }
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
		return withState {
			playlist.add(serviceFile)
			updatePreparedFileQueueUsingState()
			saveState()
		}
	}

	override fun removeFileAtPosition(position: Int): Promise<NowPlaying?> = withState {
		val promisedSkip = if (playlistPosition == position) {
			skipToNext()
		} else {
			Promise.empty()
		}

		promisedSkip
			.eventually {
				playlist.removeAt(position)

				if (playlistPosition > position)
					playlistPosition -= 1

				updatePreparedFileQueueUsingState()
				saveState()
			}
	}

	override fun moveFile(position: Int, newPosition: Int): Promise<NowPlaying?> {
		if (position < 0 || newPosition < 0) return getActiveNowPlaying()

		return withState {
			with(playlist) {
				if (position >= size || newPosition >= size) return@withState getActiveNowPlaying()
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
			saveState()
		}
	}

	private fun getActiveNowPlaying() = nowPlayingRepository.promiseNowPlaying(promisedPlayingState.first).keepPromise()

	private fun pausePlayback(): Promise<NowPlaying> {
		val promisedPause = activePlayer?.pause() ?: Unit.toPromise()

		isPlaying = false

		return promisedPause
			.eventually { saveState() }
	}

	private fun resumePlayback(): Promise<Unit> = withState {
		val positionedFileQueueProvider = positionedFileQueueProviders.getValue(isRepeating)
		val fileQueue = positionedFileQueueProvider.provideQueue(activeLibraryId, playlist, playlistPosition)
		val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(fileQueue)
		fileProgress.progress.then { startPlayback(preparedPlaybackQueue, it) }.unitResponse()
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
				withState {
					playlistPosition = p.playlistPosition
					fileProgress = ProgressingFile(p)
					saveState().then { np -> np?.run { onPlayingFileChanged?.onPlayingFileChanged(libraryId, p) } }
				}
			},
			{ e ->
				if (e is PreparationException) {
					withState {
						playlistPosition = e.positionedFile.playlistPosition
						fileProgress = StaticProgressedFile(Duration.ZERO.toPromise())
						saveState()
					}
				}
				onPlaylistError?.onError(e)
			},
			{
				isPlaying = false
				activePlayer = null
				changePosition(0, Duration.ZERO)
					.then { (libraryId, positionedFile) ->
						onPlaylistReset?.onPlaylistReset(libraryId, positionedFile)
						onPlaybackCompleted?.onPlaybackCompleted()
					}
			})
		return observable
	}

	private fun updatePreparedFileQueueUsingState() {
		withState {
			preparedPlaybackQueueResourceManagement.tryUpdateQueue(
				positionedFileQueueProviders.getValue(isRepeating).provideQueue(
					activeLibraryId,
					playlist,
					playlistPosition + 1
				)
			)

			Unit.toPromise()
		}
	}

	private fun saveState(): Promise<NowPlaying?> = withState {
		if (activeLibraryId.id > -1) fileProgress.progress.eventually {
			nowPlayingRepository.updateNowPlaying(
				NowPlaying(
					activeLibraryId,
					playlist,
					playlistPosition,
					it.millis,
					isRepeating,
				)
			)
		}
		else Promise.empty()
	}

	override fun close() {
		isPlaying = false
		onPlaybackStarted = null
		onPlayingFileChanged = null
		onPlaylistError = null
		activePlayer = null
		playbackSubscription?.dispose()
	}

	private fun <T> withState(action: PlayingState.() -> Promise<T>) : Promise<T> = synchronized(playingStateSync) {
		promisedPlayingState.second.eventually {
			synchronized(it) { it.action() }
		}
	}

	private fun updateLibraryState(libraryId: LibraryId, updateFunc: PlayingState.(LibraryId) -> Promise<PlayingState?>) =
		synchronized(playingStateSync) {
			val originalActiveLibraryId = promisedPlayingState.first
			promisedPlayingState.second.eventually { originalState ->
				if (originalState.activeLibraryId == originalActiveLibraryId) {
					originalState
						.updateFunc(libraryId)
						.then(
							{ newState -> newState ?: originalState },
							{ e ->
								logger.warn("There was an error updating the playing state, returning to original state", e)
								originalState
							})
				} else {
					originalState.toPromise()
				}
			}.also { promisedPlayingState = Pair(libraryId, it) }
		}

	private class PlayingState(
		val activeLibraryId: LibraryId,
		var playlist: MutableList<ServiceFile>,
		var isRepeating: Boolean,
		var playlistPosition: Int,
		var fileProgress: ReadFileProgress
	)

	private class StaticProgressedFile(override val progress: Promise<Duration>) : ReadFileProgress

	private class ProgressingFile(val positionedPlayingFile: PositionedPlayingFile)
		: ReadFileProgress {

		override val progress: Promise<Duration>
			get() = positionedPlayingFile.playingFile.promisePlayedFile().progress.then { it ?: Duration.ZERO }
	}
}
