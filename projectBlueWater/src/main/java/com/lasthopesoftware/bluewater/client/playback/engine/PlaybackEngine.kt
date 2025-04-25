package com.lasthopesoftware.bluewater.client.playback.engine

import androidx.lifecycle.AtomicReference
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
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.ProvidePositionedFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.playlist.ManagePlaylistPlayback
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.update
import com.lasthopesoftware.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.promises.ContinuingResult
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.regardless
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.errors.RejectionDropper
import com.namehillsoftware.handoff.promises.Promise
import org.jetbrains.annotations.Contract
import org.joda.time.Duration
import java.util.concurrent.CancellationException
import kotlin.math.max

class PlaybackEngine(
	private val preparedPlaybackQueueResourceManagement: ManagePlaybackQueues,
	positionedFileQueueProviders: Iterable<ProvidePositionedFileQueue>,
	private val nowPlayingRepository: ManageNowPlayingState,
	private val playbackBootstrapper: IStartPlayback,
) :
	ChangePlaybackState,
	ChangePlaybackStateForSystem,
	ChangePlaylistPosition,
	ChangePlaybackContinuity,
	ChangePlaylistFiles,
	RegisterPlaybackEngineEvents,
	PromisingCloseable {
	companion object {
		private val logger by lazyLogger<PlaybackEngine>()

		@Contract(pure = true)
		private fun getNextPosition(startingPosition: Int, playlist: Collection<ServiceFile>): Int =
			if (startingPosition < playlist.size - 1) startingPosition + 1 else 0

		@Contract(pure = true)
		private fun getPreviousPosition(startingPosition: Int): Int = max(startingPosition - 1, 0)

		private val markerLibraryId = LibraryId(-1)

		private val zeroProgressedFile by lazy {
			StaticProgressedFile(Duration.ZERO.toPromise())
		}
	}

	private val saveStateRateLimit = PromisingRateLimiter<NowPlaying?>(1)

	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	private val playingState = PlayingState(
		engineState = PlayingEngineState(
			markerLibraryId,
			false,
			mutableListOf(),
			0,
			false,
		),
		zeroProgressedFile,
	)

	private var activePlayer: ManagePlaylistPlayback? = null
	private var onPlayingFileChanged: OnPlayingFileChanged? = null
	private var onPlaylistError: OnPlaylistError? = null
	private var onPlaybackStarted: OnPlaybackStarted? = null
	private var onPlaybackPaused: OnPlaybackPaused? = null
	private var onPlaybackInterrupted: OnPlaybackInterrupted? = null
	private var onPlaybackCompleted: OnPlaybackCompleted? = null
	private var onPlaylistReset: OnPlaylistReset? = null

	val isPlaying: Boolean
		get() = playingState.engineState.get().isPlaying

	override fun restoreFromSavedState(libraryId: LibraryId): Promise<Pair<LibraryId, PositionedProgressedFile?>> {
		fun promiseState() = withState {
			with (engineState.get()) {
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
		}

		return withState {
			pausePlayback()
				.eventually {
					val originalState = engineState.get()
					nowPlayingRepository
						.promiseNowPlaying(libraryId)
						.then { nowPlaying ->
							nowPlaying
								?.run {
									val isUpdated = engineState.compareAndSet(
										originalState,
										PlayingEngineState(
											libraryId,
											isPlaying,
											playlist,
											playlistPosition,
											isRepeating,
										)
									)

									if (isUpdated)
										fileProgress = StaticProgressedFile(Duration.millis(filePosition).toPromise())
								}
						}
				}
				.eventually {
					promiseState()
				}
		}
	}

	override fun startPlaylist(libraryId: LibraryId, playlist: List<ServiceFile>, playlistPosition: Int, filePosition: Duration): Promise<Unit> {
		logger.info("Starting playback")
		return withState {
			val expectedState = engineState.get().copy(
				libraryId = libraryId,
				playlist = playlist.toMutableList(),
				playlistPosition = playlistPosition,
				isPlaying = true,
			)

			fileProgress = StaticProgressedFile(filePosition.toPromise())
			engineState.set(expectedState)
			saveState().then { _ ->
				engineState.get() == expectedState
			}
		}
		.eventually { isUpdated ->
			if (isUpdated) resumePlayback()
			else Unit.toPromise()
		}
	}

	override fun skipToNext(): Promise<Pair<LibraryId, PositionedFile>> = withState {
		with (engineState.get()) {
			changePosition(
				getNextPosition(playlistPosition, playlist),
				Duration.ZERO
			)
		}
	}

	override fun skipToPrevious(): Promise<Pair<LibraryId, PositionedFile>> = withState {
		changePosition(
			getPreviousPosition(engineState.get().playlistPosition),
			Duration.ZERO
		)
	}

	override fun changePosition(
		playlistPosition: Int,
		filePosition: Duration
	): Promise<Pair<LibraryId, PositionedFile>> {
		activePlayer = null

		return withState {
			updateEngineState {
				Pair(copy(playlistPosition = playlistPosition), Unit).toPromise()
			}.eventually { (o, n, _) ->
				if (o != n) {
					fileProgress = StaticProgressedFile(filePosition.toPromise())
					with(saveState()) {
						if (!isPlaying) then { np ->
							with (engineState.get()) {
								Pair(libraryId, PositionedFile(playlistPosition, playlist[playlistPosition]))
							}
						} else eventually {
							with (engineState.get()) {
								val queueProvider = positionedFileQueueProviders.getValue(isRepeating)
								val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement
									.initializePreparedPlaybackQueue(
										queueProvider.provideQueue(
											libraryId,
											playlist,
											playlistPosition
										)
									)

								startPlayback(preparedPlaybackQueue, filePosition)
									.progress
									.then { p ->
										if (p is ContinuingResult) Pair(libraryId, p.current.asPositionedFile())
										else Pair(
											libraryId, PositionedFile(playlistPosition, playlist[playlistPosition])
										)
									}
							}
						}
					}
				} else {
					with (engineState.get()) {
						Pair(libraryId, PositionedFile(playlistPosition, playlist[playlistPosition])).toPromise()
					}
				}
			}
		}
	}

	override fun playRepeatedly(): Promise<Unit> = withState {
		updateEngineState {
			Pair(copy(isRepeating = true), Unit).toPromise()
		}.eventually { (old, new, _) ->
			if (old != new) {
				val promisedSave = saveState()
				updatePreparedFileQueueUsingState()
				promisedSave.unitResponse()
			} else {
				Unit.toPromise()
			}
		}
	}

	override fun playToCompletion(): Promise<Unit> = withState {
		updateEngineState {
			Pair(copy(isRepeating = false), Unit).toPromise()
		}.eventually { (old, new, _) ->
			if (old != new) {
				val promisedSave = saveState()
				updatePreparedFileQueueUsingState()
				promisedSave.unitResponse()
			} else {
				Unit.toPromise()
			}
		}
	}

	override fun resume(): Promise<Unit> {
		return activePlayer
			?.resume()
			?.also {
				playingState.engineState.update { it.copy(isPlaying = true) }
				onPlaybackStarted?.onPlaybackStarted()
			}
			?.then { it -> onPlayingFileChanged?.onPlayingFileChanged(playingState.engineState.get().libraryId, it) }
			?: resumePlayback()
	}

	override fun pause(): Promise<Unit> =
		pausePlayback().then { _ -> onPlaybackPaused?.onPlaybackPaused() }

	override fun interrupt(): Promise<Unit> = withState {
		engineState.update { it.copy(isPlaying = false) }

		preparedPlaybackQueueResourceManagement.reset()
		activePlayer
			?.haltPlayback()
			.keepPromise()
			.eventually { saveState() }
			.also {
				activePlayer = null
			}
			.then { _ -> onPlaybackInterrupted?.onPlaybackInterrupted() }
	}.unitResponse()

	override fun addFile(serviceFile: ServiceFile): Promise<NowPlaying?> {
		return withState {
			engineState.update { it.copy(playlist = it.playlist + serviceFile) }
			updatePreparedFileQueueUsingState()
			saveState()
		}
	}

	override fun removeFileAtPosition(position: Int): Promise<NowPlaying?> = withState {
		var expectedState = engineState.get()
		val promisedSkip = if (expectedState.playlistPosition == position) {
			expectedState = expectedState.copy(playlistPosition = getNextPosition(position, expectedState.playlist))
			skipToNext()
		} else {
			Promise.empty()
		}

		promisedSkip
			.then { _ ->
				val currentState = engineState.get()
				if (expectedState == currentState) with (currentState) {
					val newPlaylist = playlist.filterIndexed { index, _ -> index != position }

					var newPosition = playlistPosition
					if (playlistPosition > position)
						newPosition -= 1

					engineState.compareAndSet(currentState, copy(playlist = newPlaylist, playlistPosition = newPosition))
				}
			}
			.eventually {
				updatePreparedFileQueueUsingState()
				saveState()
			}
	}

	override fun moveFile(position: Int, newPosition: Int): Promise<NowPlaying?> {
		if (position < 0 || newPosition < 0) return getActiveNowPlaying()

		return withState {
			val currentPlaylist = engineState.get().playlist
			engineState.update {
				if (it.playlist !== currentPlaylist || currentPlaylist.run { position >= size || newPosition >= size }) return getActiveNowPlaying()

				val newPlaylist = currentPlaylist.toMutableList()
				val removedFile = newPlaylist.removeAt(position)
				newPlaylist.add(newPosition, removedFile)

				val newPlaylistPosition = when (it.playlistPosition) {
					position -> newPosition
					in newPosition until position -> it.playlistPosition + 1
					in position..newPosition -> it.playlistPosition - 1
					else -> it.playlistPosition
				}

				it.copy(playlist = newPlaylist, playlistPosition = newPlaylistPosition)
			}

			updatePreparedFileQueueUsingState()
			saveState()
		}
	}

	override fun clearPlaylist(): Promise<NowPlaying?> {
		preparedPlaybackQueueResourceManagement.reset()
		val expectedState = playingState.engineState.get()
		return activePlayer
			?.haltPlayback()
			.keepPromise()
			.eventually {
				withState {
					with (engineState.get()) {
						if (expectedState == this) {
							activePlayer = null

							val isUpdated = engineState.compareAndSet(
								expectedState,
								copy(
									isPlaying = false,
									playlist = emptyList(),
									playlistPosition = 0
								)
							)

							if (isUpdated)
								fileProgress = zeroProgressedFile
							updatePreparedFileQueueUsingState()
							saveState()
						} else getActiveNowPlaying()
					}
				}
				.then { s ->
					onPlaybackCompleted?.onPlaybackCompleted()
					s
				}
				.keepPromise()
			}
	}

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

	private fun getActiveNowPlaying() = nowPlayingRepository
		.promiseNowPlaying(playingState.engineState.get().libraryId)
		.keepPromise()

	private fun pausePlayback(): Promise<NowPlaying?> = withState {
		updateEngineState {
			Pair(copy(isPlaying = false), Unit).toPromise()
		}.eventually { (old, new, _) ->
			activePlayer
				?.takeIf { old != new }
				?.pause()
				.keepPromise()
				.regardless { saveState() }
		}
	}

	private fun resumePlayback(): Promise<Unit> = withState {
		val fileQueue = with (engineState.updateAndGet { it.copy(isPlaying = true) }) {
			val positionedFileQueueProvider = positionedFileQueueProviders.getValue(isRepeating)
			positionedFileQueueProvider.provideQueue(libraryId, playlist, playlistPosition)
		}

		val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(fileQueue)
		fileProgress.progress.then { it -> startPlayback(preparedPlaybackQueue, it) }.unitResponse()
	}

	private fun startPlayback(preparedPlaybackQueue: PreparedPlayableFileQueue, filePosition: Duration): ProgressingPromise<PositionedPlayingFile, Unit> {
		val newPlayer = playbackBootstrapper.startPlayback(preparedPlaybackQueue, filePosition)
		activePlayer = newPlayer

		playingState.engineState.update { it.copy(isPlaying = true) }
		onPlaybackStarted?.onPlaybackStarted()

		val promisedPlayback = newPlayer.promisePlayedPlaylist()

		promisedPlayback
			.onEach { p ->
				playingState.engineState.update { it.copy(isPlaying = true) }
				withState {
					engineState.update { it.copy(playlistPosition = p.playlistPosition) }
					fileProgress = ProgressingFile(p)
					saveState().then { np -> np?.run { onPlayingFileChanged?.onPlayingFileChanged(libraryId, p) } }
				}
			}
			.then { _ ->
				playingState.engineState.update { it.copy(isPlaying = false) }
				activePlayer = null
				changePosition(0, Duration.ZERO)
					.then { (libraryId, positionedFile) ->
						onPlaylistReset?.onPlaylistReset(libraryId, positionedFile)
						onPlaybackCompleted?.onPlaybackCompleted()
					}
			}
			.excuse { e ->
				when (e) {
					is PreparationException -> {
						if (e.cause is CancellationException) {
							logger.debug("Preparation was cancelled, expecting cancellation caller to handle resource clean-up.", e)
							return@excuse
						}

						withState {
							engineState.update { it.copy(playlistPosition = e.positionedFile.playlistPosition) }
							fileProgress = zeroProgressedFile
							saveState()
						}
					}

					is PlaybackException -> {
						saveState()
					}
				}

				activePlayer?.haltPlayback()?.excuse(RejectionDropper.Instance.get())
				playingState.engineState.update { it.copy(isPlaying = false) }
				activePlayer = null

				onPlaylistError?.onError(e)
			}

		return promisedPlayback
	}

	private fun updatePreparedFileQueueUsingState() {
		withState {
			with (engineState.get()) {
				preparedPlaybackQueueResourceManagement.tryUpdateQueue(
					positionedFileQueueProviders.getValue(isRepeating).provideQueue(
						libraryId,
						playlist,
						playlistPosition + 1
					)
				)
			}

			Unit.toPromise()
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun saveState(): Promise<NowPlaying?> = withState {
		with (engineState.get()) {
			if (libraryId.id > -1) saveStateRateLimit.limit {
				fileProgress.progress.eventually {
					nowPlayingRepository.updateNowPlaying(
						NowPlaying(
							libraryId,
							playlist,
							playlistPosition,
							it.millis,
							isRepeating,
						)
					) as Promise<NowPlaying?>
				}
			} else Promise.empty()
		}
	}

	override fun promiseClose(): Promise<Unit> {
		preparedPlaybackQueueResourceManagement.reset()
		return activePlayer
			?.haltPlayback()
			.keepPromise()
			.then { _ ->
				playingState.engineState.update { it.copy(isPlaying = false) }
				onPlaybackStarted = null
				onPlayingFileChanged = null
				onPlaylistError = null
				onPlaybackInterrupted = null
				onPlaylistReset = null
				onPlaybackPaused = null
				onPlaybackCompleted = null
				activePlayer = null
			}
	}

	private inline fun <Resolution> PlayingState.updateEngineState(action: PlayingEngineState.() -> Promise<Pair<PlayingEngineState, Resolution>>): Promise<Triple<PlayingEngineState, PlayingEngineState, Resolution>> =
		engineState.get().let { currentState ->
			action(currentState)
				.then { (newState, resolution) ->
					engineState.set(newState)
					Triple(currentState, engineState.get(), resolution)
				}
		}

	private inline fun <T> withState(action: PlayingState.() -> Promise<T>): Promise<T> = playingState.run(action)

	private data class PlayingEngineState(
		val libraryId: LibraryId,
		val isPlaying: Boolean,
		val playlist: List<ServiceFile>,
		val playlistPosition: Int,
		val isRepeating: Boolean,
	)

	private class PlayingState(
		engineState: PlayingEngineState,

		@Volatile
		var fileProgress: ReadFileProgress
	) {
		val engineState = AtomicReference(engineState)
	}

	private class StaticProgressedFile(override val progress: Promise<Duration>) : ReadFileProgress

	private class ProgressingFile(val positionedPlayingFile: PositionedPlayingFile) : ReadFileProgress {

		override val progress: Promise<Duration>
			get() = positionedPlayingFile.playingFile.promisePlayedFile().progress.then { it -> it ?: Duration.ZERO }
	}
}
