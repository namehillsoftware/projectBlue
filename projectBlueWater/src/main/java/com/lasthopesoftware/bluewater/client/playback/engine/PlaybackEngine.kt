package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.BootstrapPlayback
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
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.ProvidePositionedFileQueue
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ManageNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.playlist.ManagePlaylistPlayback
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.ContinuingResult
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.onEachEventually
import com.lasthopesoftware.promises.extensions.regardless
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.errors.RejectionDropper
import com.namehillsoftware.handoff.promises.Promise
import org.jetbrains.annotations.Contract
import org.joda.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class PlaybackEngine(
	private val preparedPlaybackQueueResourceManagement: ManagePlaybackQueues,
	positionedFileQueueProviders: Iterable<ProvidePositionedFileQueue>,
	private val nowPlayingRepository: ManageNowPlayingState,
	private val playbackBootstrapper: BootstrapPlayback,
	private val playlistPlayback: ManagePlaylistPlayback,
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
		private val logger by lazyLogger<PlaybackEngine>()

		@Contract(pure = true)
		private fun getNextPosition(startingPosition: Int, playlist: Collection<ServiceFile>): Int =
			if (startingPosition < playlist.size - 1) startingPosition + 1 else 0

		@Contract(pure = true)
		private fun getPreviousPosition(startingPosition: Int): Int = max(startingPosition - 1, 0)

		private val markerLibraryId = LibraryId(-1)
	}

	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	private val promisedNowPlayingState = AtomicReference(Promise.empty<NowPlaying?>())
	private val activeLibraryId = AtomicReference(markerLibraryId)
	private val playerSessionCounter = AtomicInteger(Int.MIN_VALUE) // Counter for optimistically validating concurrency
	private val promisedPlayback = AtomicReference(null as ProgressingPromise<PositionedPlayingFile, Unit>?)

	private var onPlayingFileChanged: OnPlayingFileChanged? = null
	private var onPlaylistError: OnPlaylistError? = null
	private var onPlaybackStarted: OnPlaybackStarted? = null
	private var onPlaybackPaused: OnPlaybackPaused? = null
	private var onPlaybackInterrupted: OnPlaybackInterrupted? = null
	private var onPlaybackCompleted: OnPlaybackCompleted? = null
	private var onPlaylistReset: OnPlaylistReset? = null

	@Volatile
	var isPlaying: Boolean = false
		private set

	override fun restoreFromSavedState(libraryId: LibraryId): Promise<Pair<LibraryId, PositionedProgressedFile?>> {
		fun pauseAndSaveLibraryState(originalLibraryId: LibraryId): Promise<*> =
			playlistPlayback
				.pause()
				.eventually { pf ->
					pf?.playableFile
						?.progress
						?.eventually { p -> saveState(originalLibraryId, filePosition = p.millis) }
						.keepPromise()
				}

		fun NowPlaying.toState() =
			Pair(
				libraryId,
				playingFile
					?.run {
						PositionedProgressedFile(
							playlistPosition,
							serviceFile,
							Duration.millis(filePosition)
						)
					}
			)

		fun promiseStoredState(): Promise<Pair<LibraryId, PositionedProgressedFile?>> =
			promiseActiveNowPlaying()
				.then { maybeNp -> maybeNp?.toState() }

		fun promiseState(): Promise<Pair<LibraryId, PositionedProgressedFile?>> = promisedPlayback.get()
			?.progress
			?.eventually { playlistProgress ->
				playlistProgress
					.let { it as? ContinuingResult }
					?.current
					?.let {
						it.playingFile.progress.then { fileProgress ->
							Pair(libraryId, PositionedProgressedFile(it.playlistPosition, it.serviceFile, fileProgress))
						}
					}
					?: promiseStoredState()
			}
			 ?: promiseStoredState()

		val originalLibraryId = activeLibraryId.getAndSet(libraryId)
		return if (originalLibraryId == libraryId) promiseState()
		else {
			playerSessionCounter.incrementAndGet()
			isPlaying = false
			promisedPlayback.set(null)

			pauseAndSaveLibraryState(originalLibraryId)
				.eventually {
					updatePlayerFromState().then { maybeNp -> maybeNp?.toState() ?: Pair(libraryId, null) }
				}
		}
	}

	override fun startPlaylist(libraryId: LibraryId, playlist: List<ServiceFile>, playlistPosition: Int): Promise<Unit> {
		logger.info("Starting playback")

		activeLibraryId.set(libraryId)
		isPlaying = true
		val playerId = playerSessionCounter.incrementAndGet()

		return saveState(
			libraryId,
			playlist = playlist,
			playlistPosition = playlistPosition,
			filePosition = 0L,
			)
			.eventually { saved ->
				if (activeLibraryId.get() != libraryId) Unit.toPromise()
				else updatePlayerFromState()
					.then { updated ->
						if (updated != null && saved == updated && isPlaying)
							startPlayback(playerId, updated)
					}
			}
	}

	override fun skipToNext(): Promise<Pair<LibraryId, PositionedFile>?> = promiseActiveNowPlaying().eventually {
		it?.run {
			changePosition(getNextPosition(playlistPosition, playlist), Duration.ZERO)
		}
	}

	override fun skipToPrevious(): Promise<Pair<LibraryId, PositionedFile>?> = promiseActiveNowPlaying().eventually {
		it?.run {
			changePosition(getPreviousPosition(playlistPosition), Duration.ZERO)
		}
	}

	override fun changePosition(
		playlistPosition: Int,
		filePosition: Duration
	): Promise<Pair<LibraryId, PositionedFile>?> {
		val startingLibraryId = activeLibraryId.get()
		val newPlayerId = playerSessionCounter.incrementAndGet()
		return saveState(startingLibraryId) {
			if (!newPlayerId.isCurrentPlayerId()) this
			else copy(playlistPosition = playlistPosition, filePosition = filePosition.millis)
		}
			.eventually {
				if (!newPlayerId.isCurrentPlayerId() || activeLibraryId.get() != startingLibraryId) Promise.empty()
				else updatePlayerFromState()
			}
			.eventually { np ->
				when {
					activeLibraryId.get() != startingLibraryId -> Promise.empty()
					newPlayerId != playerSessionCounter.get() -> Promise.empty()
					np?.playlistPosition != playlistPosition || np.filePosition != filePosition.millis -> Promise.empty()
					!isPlaying -> {
						promisedPlayback.set(null)
						Pair(
							startingLibraryId,
							np.playingFile ?: PositionedFile(playlistPosition, np.playlist[playlistPosition])
						).toPromise()
					}
					else -> {
						startPlayback(newPlayerId, np)
							.then({ p ->
								p?.asPositionedFile()?.let { Pair(np.libraryId, it) } ?: Pair(
									np.libraryId,
									PositionedFile(playlistPosition, np.playlist[playlistPosition])
								)
							}, { e ->
								if (e is CancellationException || e is PreparationException && e.cause is CancellationException) {
									logger.info("Playback was cancelled, returning null.", e)
									null
								} else {
									throw e
								}
							})
					}
				}
			}
	}

	override fun playRepeatedly(): Promise<Unit> = saveState(activeLibraryId.get(), isRepeating = true)
		.then(::updatePreparedFileQueueUsingState)
		.unitResponse()

	override fun playToCompletion(): Promise<Unit> = saveState(activeLibraryId.get(), isRepeating = false)
		.then(::updatePreparedFileQueueUsingState)
		.unitResponse()

	override fun resume(): Promise<Unit> {
		isPlaying = true

		return if (promisedPlayback.get() == null) {
			val currentPlayerId = playerSessionCounter.get()
			updatePlayerFromState().then { np -> np?.let { startPlayback(currentPlayerId, it) }; Unit }
		} else {
			playlistPlayback
				.resume()
				.also {
					onPlaybackStarted?.onPlaybackStarted()
				}
				.then { file ->
					onPlayingFileChanged?.onPlayingFileChanged(activeLibraryId.get(), file)
				}
		}
	}

	override fun pause(): Promise<Unit> =
		pausePlayback().then { _ -> onPlaybackPaused?.onPlaybackPaused() }

	override fun interrupt(): Promise<Unit> =
		pausePlayback().then { _ -> onPlaybackInterrupted?.onPlaybackInterrupted() }

	override fun addFile(serviceFile: ServiceFile): Promise<NowPlaying?> = saveState(activeLibraryId.get()) {
		copy(playlist = playlist + serviceFile)
	}.then(::updatePreparedFileQueueUsingState)

	override fun removeFileAtPosition(position: Int): Promise<NowPlaying?> {
		return promiseActiveNowPlaying()
			.eventually {
				if (it?.playlistPosition == position) skipToNext().then { p -> p?.first }
				else it?.libraryId.toPromise()
			}
			.eventually {
				it?.let { id ->
					saveState(id) {
						val newPlaylist = playlist.take(position) + playlist.drop(position + 1)

						var newPosition = playlistPosition
						if (playlistPosition > position)
							newPosition -= 1

						copy(playlist = newPlaylist, playlistPosition = newPosition)
					}
				}
			}
			.then(::updatePreparedFileQueueUsingState)
	}

	override fun moveFile(position: Int, newPosition: Int): Promise<NowPlaying?> {
		if (position < 0 || newPosition < 0) return promiseActiveNowPlaying()

		return saveState(activeLibraryId.get()) {
			if (playlist.run { position >= size || newPosition >= size }) return@saveState this

			val newPlaylist = playlist.toMutableList()
			val removedFile = newPlaylist.removeAt(position)
			newPlaylist.add(newPosition, removedFile)

			val newPlaylistPosition = when (playlistPosition) {
				position -> newPosition
				in newPosition until position -> playlistPosition + 1
				in position..newPosition -> playlistPosition - 1
				else -> playlistPosition
			}

			copy(playlist = newPlaylist, playlistPosition = newPlaylistPosition)
		}.then(::updatePreparedFileQueueUsingState)
	}

	override fun clearPlaylist(): Promise<NowPlaying?> {
		isPlaying = false
		preparedPlaybackQueueResourceManagement.reset()
		val newPlayerId = playerSessionCounter.incrementAndGet()
		return saveState(activeLibraryId.get(), playlist = emptyList())
			.eventually { np ->
				if (newPlayerId.isCurrentPlayerId()) {
					promisedPlayback.set(null)
					playlistPlayback
						.haltPlayback()
						.then { _ ->
							onPlaybackCompleted?.onPlaybackCompleted()
							np
						}
				} else np.toPromise()
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

	override fun close() {
		onPlaybackStarted = null
		onPlayingFileChanged = null
		onPlaylistError = null
		onPlaybackInterrupted = null
		onPlaylistReset = null
		onPlaybackPaused = null
		onPlaybackCompleted = null
	}

	private fun pausePlayback(): Promise<NowPlaying?> {
		isPlaying = false
		val libraryId = activeLibraryId.get()
		return playlistPlayback
			.pause()
			.eventually {
				it?.playableFile
					?.progress
					?.eventually { p -> saveState(libraryId, filePosition = p?.millis ?: 0) }
					.keepPromise()
			}
	}

	private fun updatePreparedFileQueueUsingState(maybeNp: NowPlaying?) = maybeNp?.apply {
		preparedPlaybackQueueResourceManagement.tryUpdateQueue(
			positionedFileQueueProviders.getValue(isRepeating).provideQueue(
				libraryId,
				playlist,
				playlistPosition + 1
			)
		)
	}

	private fun startPlayback(playerId: Int, activeNp: NowPlaying): Promise<PositionedPlayingFile?> {
		if (!playerId.isCurrentPlayerId()) return Promise.empty()

		onPlaybackStarted?.onPlaybackStarted()

		val attachedLibraryId = activeNp.libraryId

		val newPromisedPlayback = promisedPlayback.updateAndGet { playlistPlayback.promisePlayedPlaylist() }

		newPromisedPlayback
			?.onEachEventually { p ->
				if (!playerId.isCurrentPlayerId()) Unit.toPromise()
				else p.playingFile
					.progress
					.eventually { progress ->
						if (!playerId.isCurrentPlayerId()) Unit.toPromise()
						else saveState(attachedLibraryId) {
							if (!playerId.isCurrentPlayerId()) this
							else copy(
								playlistPosition = p.playlistPosition,
								filePosition = progress.millis
							)
						}.then { maybeNp ->
							maybeNp
								?.takeIf {
									playerId.isCurrentPlayerId() &&
										attachedLibraryId == activeLibraryId.get() &&
										it.playlistPosition == p.playlistPosition &&
										it.filePosition == progress.millis
								}
								?.run { onPlayingFileChanged?.onPlayingFileChanged(libraryId, p) }
						}
					}
			}
			?.then({ _ ->
				if (!playerId.isCurrentPlayerId()) return@then

				isPlaying = false
				changePosition(0, Duration.ZERO)
					.then { p ->
						if (attachedLibraryId == activeLibraryId.get()) {
							p?.also { (libraryId, positionedFile) ->
								onPlaylistReset?.onPlaylistReset(libraryId, positionedFile)
							}
							onPlaybackCompleted?.onPlaybackCompleted()
						}
					}
			}, { e ->
				if (e is CancellationException) return@then

				val promisedSave = when (e) {
					is PreparationException -> {
						if (e.cause is CancellationException) {
							logger.debug(
								"Preparation was cancelled, expecting cancellation caller to handle state clean-up.",
								e
							)
							return@then
						}

						saveState(attachedLibraryId) {
							if (playerId.isCurrentPlayerId()) copy(playlistPosition = e.positionedFile.playlistPosition)
							else this
						}
					}

					is PlaybackException -> {
						e.playbackHandler.progress.eventually { p ->
							saveState(attachedLibraryId) {
								if (playerId.isCurrentPlayerId()) copy(filePosition = p.millis)
								else this
							}
						}
					}

					else -> Promise.empty()
				}

				if (!playerId.isCurrentPlayerId()) return@then

				isPlaying = false

				playlistPlayback.haltPlayback().excuse(RejectionDropper.Instance.get())

				promisedSave
					.must { _ ->
						if (playerId.isCurrentPlayerId()) {
							promisedPlayback.set(null)
							playerSessionCounter.incrementAndGet()
							onPlaylistError?.onError(e)
						}
					}
			})

		return playlistPlayback.resume()
	}

	private fun Int.isCurrentPlayerId() = this == playerSessionCounter.get()

	private fun updatePlayerFromState() = playbackBootstrapper.updateFromState(activeLibraryId.get())

	private fun promiseActiveNowPlaying() = nowPlayingRepository.promiseNowPlaying(activeLibraryId.get())

	private fun saveState(
		libraryId: LibraryId,
		playlist: List<ServiceFile>? = null,
		playlistPosition: Int? = null,
		filePosition: Long? = null,
		isRepeating: Boolean? = null
	): Promise<NowPlaying?> = saveState(libraryId) {
		copy(
			playlist = playlist ?: this.playlist,
			playlistPosition = playlistPosition ?: this.playlistPosition,
			filePosition = filePosition ?: this.filePosition,
			isRepeating = isRepeating ?: this.isRepeating
		)
	}

	private inline fun saveState(
		libraryId: LibraryId,
		crossinline updateFunc: NowPlaying.() -> NowPlaying
	): Promise<NowPlaying?> = updateStateSynchronously {
		nowPlayingRepository.promiseNowPlaying(libraryId).eventually {
			it?.let { np ->
				val updatedNowPlaying = updateFunc(np)
				if (updatedNowPlaying !== np) nowPlayingRepository.updateNowPlaying(updatedNowPlaying)
				else np.toPromise()
			}.keepPromise()
		}
	}

	private inline fun updateStateSynchronously(crossinline updateFunc: () -> Promise<NowPlaying?>): Promise<NowPlaying?> =
		promisedNowPlayingState.updateAndGet { it.regardless { updateFunc() } }
}
