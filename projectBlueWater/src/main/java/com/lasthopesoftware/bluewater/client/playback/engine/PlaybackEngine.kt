package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.IStartPlayback
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackCompleted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackStarted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistReset
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ManagePlaybackQueues
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueueProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.VoidResponse
import com.vedsoft.futures.runnables.OneParameterAction
import io.reactivex.disposables.Disposable
import io.reactivex.observables.ConnectableObservable
import org.jetbrains.annotations.Contract
import org.slf4j.LoggerFactory
import kotlin.math.max

class PlaybackEngine(managePlaybackQueues: ManagePlaybackQueues, positionedFileQueueProviders: Iterable<IPositionedFileQueueProvider>, private val nowPlayingRepository: INowPlayingRepository, private val playbackBootstrapper: IStartPlayback) : IChangePlaylistPosition, IPlaybackEngineBroadcaster, AutoCloseable {
	private val preparedPlaybackQueueResourceManagement = managePlaybackQueues
	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	private var positionedPlayingFile: PositionedPlayingFile? = null
	private var playlist: MutableList<ServiceFile>? = null
	var isPlaying = false
		private set

	private var playbackSubscription: Disposable? = null
	private var activePlayer: IActivePlayer? = null
	private var onPlayingFileChanged: OnPlayingFileChanged? = null
	private var onPlaylistError: OneParameterAction<Throwable>? = null
	private var onPlaybackStarted: OnPlaybackStarted? = null
	private var onPlaybackCompleted: OnPlaybackCompleted? = null
	private var onPlaylistReset: OnPlaylistReset? = null

	fun startPlaylist(playlist: MutableList<ServiceFile>?, playlistPosition: Int, filePosition: Int): Promise<Void> {
		logger.info("Starting playback")
		this.playlist = playlist
		return updateLibraryPlaylistPositions(playlistPosition, filePosition).then(VoidResponse { resumePlaybackFromNowPlaying(it) })
	}

	fun skipToNext(): Promise<PositionedFile> {
		return nowPlayingRepository
			.nowPlaying
			.eventually { np -> changePosition(getNextPosition(np.playlistPosition, np.playlist), 0) }
	}

	fun skipToPrevious(): Promise<PositionedFile> {
		return nowPlayingRepository
			.nowPlaying
			.eventually { np -> changePosition(getPreviousPosition(np.playlistPosition), 0) }
	}

	@Synchronized
	override fun changePosition(playlistPosition: Int, filePosition: Int): Promise<PositionedFile> {
		playbackSubscription?.dispose()
		activePlayer = null

		val nowPlayingPromise = updateLibraryPlaylistPositions(playlistPosition, filePosition)
			.then {
				logger.info("Position changed")
				it
			}

		return with(nowPlayingPromise) {
			if (!isPlaying) then { nowPlaying ->
				val serviceFile = nowPlaying.playlist[playlistPosition]
				PositionedFile(playlistPosition, serviceFile)
			} else eventually { nowPlaying ->
				object : Promise<PositionedFile>() {
					init {
						val queueProvider = positionedFileQueueProviders.getValue(nowPlaying.isRepeating)
						try {
							val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement
								.initializePreparedPlaybackQueue(queueProvider.provideQueue(playlist, playlistPosition))
							startPlayback(preparedPlaybackQueue, filePosition.toLong())
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

	fun playRepeatedly() {
		persistLibraryRepeating(true)
		updatePreparedFileQueueUsingState(positionedFileQueueProviders.getValue(true))
	}

	fun playToCompletion() {
		persistLibraryRepeating(false)
		updatePreparedFileQueueUsingState(positionedFileQueueProviders.getValue(false))
	}

	fun resume(): Promise<*> {
		return activePlayer
			?.also {
				isPlaying = true
				onPlaybackStarted?.onPlaybackStarted()
			}
			?.resume()?.then { onPlayingFileChanged?.onPlayingFileChanged(it) }
			?: return restorePlaylistFromStorage().then { resumePlaybackFromNowPlaying(it) }
	}

	fun pause(): Promise<*> {
		val promisedPause = activePlayer?.pause() ?: Promise.empty<Any?>()

		isPlaying = false

		val currentPlayingFile = positionedPlayingFile ?: return promisedPause

		return promisedPause.eventually { saveStateToLibrary(currentPlayingFile) }
	}

	override fun setOnPlayingFileChanged(onPlayingFileChanged: OnPlayingFileChanged): PlaybackEngine {
		this.onPlayingFileChanged = onPlayingFileChanged
		return this
	}

	override fun setOnPlaylistError(onPlaylistError: OneParameterAction<Throwable>): PlaybackEngine {
		this.onPlaylistError = onPlaylistError
		return this
	}

	override fun setOnPlaybackStarted(onPlaybackStarted: OnPlaybackStarted): PlaybackEngine {
		this.onPlaybackStarted = onPlaybackStarted
		return this
	}

	override fun setOnPlaybackCompleted(onPlaybackCompleted: OnPlaybackCompleted): PlaybackEngine {
		this.onPlaybackCompleted = onPlaybackCompleted
		return this
	}

	override fun setOnPlaylistReset(onPlaylistReset: OnPlaylistReset): PlaybackEngine {
		this.onPlaylistReset = onPlaylistReset
		return this
	}

	private fun resumePlaybackFromNowPlaying(nowPlaying: NowPlaying) {
		val positionedFileQueueProvider = positionedFileQueueProviders.getValue(nowPlaying.isRepeating)
		val fileQueue = positionedFileQueueProvider.provideQueue(nowPlaying.playlist, nowPlaying.playlistPosition)
		val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(fileQueue)
		startPlayback(preparedPlaybackQueue, nowPlaying.filePosition)
	}

	private fun startPlayback(preparedPlaybackQueue: PreparedPlayableFileQueue, filePosition: Long): ConnectableObservable<PositionedPlayingFile> {
		playbackSubscription?.dispose()

		val newPlayer = playbackBootstrapper.startPlayback(preparedPlaybackQueue, filePosition)
		activePlayer = newPlayer

		isPlaying = true
		onPlaybackStarted?.onPlaybackStarted()

		val observable = newPlayer.observe()

		playbackSubscription = observable.subscribe(
			{ p ->
				isPlaying = true
				positionedPlayingFile = p
				onPlayingFileChanged?.onPlayingFileChanged(p)
				saveStateToLibrary(p)
			},
			{ e ->
				if (e is PreparationException) {
					saveStateToLibrary(
						PositionedPlayingFile(
							EmptyPlaybackHandler(0),
							NoTransformVolumeManager(),
							e.positionedFile))
				}
				onPlaylistError?.runWith(e)
			},
			{
				isPlaying = false
				positionedPlayingFile = null
				activePlayer = null
				changePosition(0, 0)
					.then { positionedFile ->
						onPlaylistReset?.onPlaylistReset(positionedFile)
						onPlaybackCompleted?.onPlaybackCompleted()
					}
			})
		return observable
	}

	fun addFile(serviceFile: ServiceFile): Promise<NowPlaying> {
		return nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				np.playlist.add(serviceFile)

				val nowPlayingPromise = nowPlayingRepository.updateNowPlaying(np)

				playlist?.add(serviceFile)?.let {
					updatePreparedFileQueueUsingState(positionedFileQueueProviders.getValue(np.isRepeating))
					nowPlayingPromise
				} ?: nowPlayingPromise
			}
	}

	fun removeFileAtPosition(position: Int): Promise<NowPlaying?> {
		return nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				if (np.playlistPosition == position)
					skipToNext().eventually { nowPlayingRepository.nowPlaying }
				else
					np.toPromise()
			}
			.eventually { np ->
				np.playlist.removeAt(position)

				if (np.playlistPosition > position) {
					np.playlistPosition--

					positionedPlayingFile?.let {
						positionedPlayingFile = PositionedPlayingFile(
							it.playlistPosition - 1,
							it.playingFile,
							it.playableFileVolumeManager,
							it.serviceFile)
					}
				}

				val libraryUpdatePromise = nowPlayingRepository.updateNowPlaying(np)

				playlist?.removeAt(position)?.let {
					updatePreparedFileQueueUsingState(positionedFileQueueProviders.getValue(np.isRepeating))
					libraryUpdatePromise
				} ?: libraryUpdatePromise
			}
	}

	private fun updatePreparedFileQueueUsingState(fileQueueProvider: IPositionedFileQueueProvider) {
		playlist?.let { pl ->
			positionedPlayingFile?.let { f ->
				preparedPlaybackQueueResourceManagement
					.tryUpdateQueue(fileQueueProvider.provideQueue(pl, f.playlistPosition + 1))
			}
		}
	}

	private fun updateLibraryPlaylistPositions(playlistPosition: Int, filePosition: Int): Promise<NowPlaying> {
		val nowPlayingPromise = if (playlist != null) nowPlayingRepository.nowPlaying else restorePlaylistFromStorage()
		return nowPlayingPromise
			.eventually { np ->
				np.playlist = playlist
				np.playlistPosition = playlistPosition
				np.filePosition = filePosition.toLong()
				nowPlayingRepository.updateNowPlaying(np)
			}
	}

	private fun restorePlaylistFromStorage(): Promise<NowPlaying> {
		return nowPlayingRepository
			.nowPlaying
			.then { np ->
				playlist = np.playlist
				np
			}
	}

	private fun persistLibraryRepeating(isRepeating: Boolean): Promise<NowPlaying> {
		return nowPlayingRepository
			.nowPlaying
			.then { result ->
				result.isRepeating = isRepeating
				nowPlayingRepository.updateNowPlaying(result)
				result
			}
	}

	private fun saveStateToLibrary(positionedPlayingFile: PositionedPlayingFile): Promise<NowPlaying> {
		return if (playlist == null) Promise.empty() else nowPlayingRepository
			.nowPlaying
			.then { np ->
				np.playlist = playlist
				np.playlistPosition = positionedPlayingFile.playlistPosition
				np.filePosition = positionedPlayingFile
					.playingFile
					.promisePlayedFile()
					.progress?.millis ?: 0
				np
			}
			.eventually { nowPlayingRepository.updateNowPlaying(it) }
	}

	override fun close() {
		isPlaying = false
		onPlaybackStarted = null
		onPlayingFileChanged = null
		onPlaylistError = null
		playbackSubscription?.dispose()
		activePlayer = null
		positionedPlayingFile = null
		playlist = null
	}

	companion object {
		private val logger = LoggerFactory.getLogger(PlaybackEngine::class.java)

		@Contract(pure = true)
		private fun getNextPosition(startingPosition: Int, playlist: Collection<ServiceFile>): Int {
			return if (startingPosition < playlist.size - 1) startingPosition + 1 else 0
		}

		@Contract(pure = true)
		private fun getPreviousPosition(startingPosition: Int): Int {
			return max(startingPosition - 1, 0)
		}
	}
}
