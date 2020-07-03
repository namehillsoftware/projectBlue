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
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueueProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.vedsoft.futures.runnables.OneParameterAction
import io.reactivex.disposables.Disposable
import io.reactivex.observables.ConnectableObservable
import org.jetbrains.annotations.Contract
import org.slf4j.LoggerFactory
import kotlin.math.max

class PlaybackEngine(
	managePlaybackQueues: ManagePlaybackQueues,
	positionedFileQueueProviders: Iterable<IPositionedFileQueueProvider>,
	private val nowPlayingRepository: INowPlayingRepository,
	private val playbackBootstrapper: IStartPlayback,
	private var playlist: MutableList<ServiceFile>,
	private var positionedProgressedFile: PositionedProgressedFile,
	private var isRepeating: Boolean) :
		IChangePlaylistPosition,
		IPlaybackEngineBroadcaster,
		AutoCloseable {
	private val preparedPlaybackQueueResourceManagement = managePlaybackQueues
	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	var isPlaying = false
		private set

	private var playbackSubscription: Disposable? = null
	private var activePlayer: IActivePlayer? = null
	private var onPlayingFileChanged: OnPlayingFileChanged? = null
	private var onPlaylistError: OneParameterAction<Throwable>? = null
	private var onPlaybackStarted: OnPlaybackStarted? = null
	private var onPlaybackCompleted: OnPlaybackCompleted? = null
	private var onPlaylistReset: OnPlaylistReset? = null

	fun startPlaylist(playlist: MutableList<ServiceFile>, playlistPosition: Int, filePosition: Int): Promise<*> {
		logger.info("Starting playback")
		this.playlist = playlist
		this.positionedProgressedFile = PositionedProgressedFile(
			playlistPosition,
			filePosition.toLong(),
			playlist[playlistPosition])
		return saveStateToLibrary().then { resumePlayback() }
	}

	fun skipToNext(): Promise<PositionedFile> {
		return changePosition(
			getNextPosition(positionedProgressedFile.playlistProgress, playlist),
			0)
	}

	fun skipToPrevious(): Promise<PositionedFile> {
		return changePosition(getPreviousPosition(positionedProgressedFile.playlistProgress), 0)
	}

	@Synchronized
	override fun changePosition(playlistPosition: Int, filePosition: Int): Promise<PositionedFile> {
		playbackSubscription?.dispose()
		activePlayer = null

		return if (!isPlaying) {
			val serviceFile = playlist[playlistPosition]
			PositionedFile(playlistPosition, serviceFile).toPromise()
		} else {
			object : Promise<PositionedFile>() {
				init {
					val queueProvider = positionedFileQueueProviders.getValue(isRepeating)
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

	fun playRepeatedly() {
		isRepeating = true
		saveStateToLibrary()
		updatePreparedFileQueueUsingState()
	}

	fun playToCompletion() {
		isRepeating = false
		saveStateToLibrary()
		updatePreparedFileQueueUsingState()
	}

	fun resume(): Promise<*> {
		return activePlayer
			?.also {
				isPlaying = true
				onPlaybackStarted?.onPlaybackStarted()
			}
			?.resume()?.then { onPlayingFileChanged?.onPlayingFileChanged(it) }
			?: return resumePlayback().toPromise()
	}

	fun pause(): Promise<*> {
		val promisedPause = activePlayer?.pause() ?: Promise.empty<Any?>()

		isPlaying = false

		return promisedPause.eventually { saveStateToLibrary() }
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

	fun addFile(serviceFile: ServiceFile): Promise<NowPlaying> {
		playlist.add(serviceFile)
		updatePreparedFileQueueUsingState()
		return saveStateToLibrary()
	}

	fun removeFileAtPosition(position: Int): Promise<NowPlaying> {
		val promisedSkip = if (positionedProgressedFile.playlistProgress == position) {
			skipToNext()
		} else {
			Promise.empty()
		}

		return promisedSkip
			.eventually {
				playlist.removeAt(position)

				if (positionedProgressedFile.playlistProgress > position) {
					positionedProgressedFile.let {
						positionedProgressedFile = PositionedProgressedFile(
							it.playlistProgress - 1,
							it.fileProgress,
							it.serviceFile)
					}
				}

				updatePreparedFileQueueUsingState()
				saveStateToLibrary()
			}
	}

	private fun resumePlayback() {
		val positionedFileQueueProvider = positionedFileQueueProviders.getValue(isRepeating)
		val fileQueue = positionedFileQueueProvider.provideQueue(playlist, positionedProgressedFile.playlistProgress)
		val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(fileQueue)
		startPlayback(preparedPlaybackQueue, positionedProgressedFile.fileProgress)
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
				positionedProgressedFile = PositionedProgressedFile(
					p.playlistPosition,
					p.playingFile.promisePlayedFile().progress?.millis ?: 0,
					p.serviceFile)
				onPlayingFileChanged?.onPlayingFileChanged(p)
				saveStateToLibrary()
			},
			{ e ->
				if (e is PreparationException) {
					saveStateToLibrary()
				}
				onPlaylistError?.runWith(e)
			},
			{
				isPlaying = false
				activePlayer = null
				changePosition(0, 0)
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
				positionedProgressedFile.playlistProgress + 1))
	}

	private fun saveStateToLibrary(): Promise<NowPlaying> {
		return nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				np.playlist = playlist
				np.playlistPosition = positionedProgressedFile.playlistProgress
				np.filePosition = it.playingFile
					.promisePlayedFile()
					.progress.millis ?: 0
				nowPlayingRepository.updateNowPlaying(np)
			}
	}

	override fun close() {
		isPlaying = false
		onPlaybackStarted = null
		onPlayingFileChanged = null
		onPlaylistError = null
		playbackSubscription?.dispose()
		activePlayer = null
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

		@JvmStatic
		fun createEngine(managePlaybackQueues: ManagePlaybackQueues, positionedFileQueueProviders: Iterable<IPositionedFileQueueProvider>, nowPlayingRepository: INowPlayingRepository, playbackBootstrapper: IStartPlayback): Promise<PlaybackEngine> {
			return nowPlayingRepository
				.nowPlaying
				.then { np ->
					PlaybackEngine(
						managePlaybackQueues,
						positionedFileQueueProviders,
						nowPlayingRepository,
						playbackBootstrapper,
						np.playlist,
						PositionedProgressedFile(
							np.playlistPosition,
							np.filePosition,
							np.playlist[np.playlistPosition]),
						np.isRepeating)
				}
		}
	}
}
