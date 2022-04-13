package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue
import com.lasthopesoftware.bluewater.shared.drainQueue
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock

class PreparedPlayableFileQueue(
	private val configuration: IPreparedPlaybackQueueConfiguration,
	private val playbackPreparer: PlayableFilePreparationSource,
	private var positionedFileQueue: IPositionedFileQueue) : SupplyQueuedPreparedFiles, Closeable {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(PreparedPlayableFileQueue::class.java) }
	}

	private val queueUpdateLock = ReentrantReadWriteLock()
	private val bufferingMediaPlayerPromises = ConcurrentLinkedQueue<ProvidePreparedPlaybackFile>()
	private var currentPreparingPlaybackHandlerPromise: ProvidePreparedPlaybackFile? = null

	fun updateQueue(newPositionedFileQueue: IPositionedFileQueue): PreparedPlayableFileQueue {
		val writeLock = queueUpdateLock.writeLock()
		writeLock.lock()
		return try {
			val newPositionedPreparingMediaPlayerPromises = LinkedList<ProvidePreparedPlaybackFile>()

			for (positionedPreparingFile in bufferingMediaPlayerPromises.drainQueue()) {
				if (positionedPreparingFile.positionedFile == newPositionedFileQueue.peek()) {
					newPositionedPreparingMediaPlayerPromises.offer(positionedPreparingFile)
					newPositionedFileQueue.poll()
					continue
				}

				for (file in bufferingMediaPlayerPromises.drainQueue()) {
					file.preparedPlaybackFilePromise.cancel()
				}
			}

			for (positionedPreparingFile in newPositionedPreparingMediaPlayerPromises.drainQueue()) {
				bufferingMediaPlayerPromises.offer(positionedPreparingFile)
			}

			positionedFileQueue = newPositionedFileQueue
			beginQueueingPreparingPlayers()
			this
		} finally {
			writeLock.unlock()
		}
	}

	override fun promiseNextPreparedPlaybackFile(preparedAt: Duration): Promise<PositionedPlayableFile>? {
		return bufferingMediaPlayerPromises.poll()?.let {
			currentPreparingPlaybackHandlerPromise = it
			Promise
				.whenAny(
					it.promisePositionedPreparedPlaybackFile(),
					Promise(PositionedPreparedPlayableFile.emptyHandler(it.positionedFile))
				)
				.eventually(::preparePlayableFileAgainIfNecessary)
				.then(::toPositionedPlayableFile)
		} ?: getNextFaultingPreparingMediaPlayerPromise(preparedAt).let {
			currentPreparingPlaybackHandlerPromise = it
			it?.promisePositionedPreparedPlaybackFile()?.then(::toPositionedPlayableFile)
		}
	}

	override fun close() {
		currentPreparingPlaybackHandlerPromise?.preparedPlaybackFilePromise?.cancel()

		val writeLock = queueUpdateLock.writeLock()
		writeLock.lock()
		try {
			for (positionedPreparingFile in bufferingMediaPlayerPromises.drainQueue())
				positionedPreparingFile.preparedPlaybackFilePromise.cancel()
		} finally {
			writeLock.unlock()
		}
	}

	private fun getNextUnfaultingPreparingMediaPlayerPromise(): PositionedUnfaultingPreparingFile? {
		val writeLock = queueUpdateLock.writeLock()
		writeLock.lock()
		return try {
			positionedFileQueue.poll()
		} finally {
			writeLock.unlock()
		}?.let {
			PositionedUnfaultingPreparingFile(
				it,
				playbackPreparer.promisePreparedPlaybackFile(it.serviceFile, Duration.ZERO))
		}
	}

	private fun getNextFaultingPreparingMediaPlayerPromise(preparedAt: Duration): PositionedPreparingFile? {
		val writeLock = queueUpdateLock.writeLock()
		writeLock.lock()
		return try {
			positionedFileQueue.poll()
		} finally {
			writeLock.unlock()
		}?.let {
			PositionedPreparingFile(
				it,
				playbackPreparer.promisePreparedPlaybackFile(it.serviceFile, preparedAt))
		}
	}

	private fun beginQueueingPreparingPlayers() {
		val writeLock = queueUpdateLock.writeLock()
		writeLock.lock()
		try {
			if (bufferingMediaPlayerPromises.size >= configuration.maxQueueSize) return
			val nextPreparingMediaPlayerPromise = getNextUnfaultingPreparingMediaPlayerPromise() ?: return
			bufferingMediaPlayerPromises.offer(nextPreparingMediaPlayerPromise)
		} finally {
			writeLock.unlock()
		}
	}

	private fun toPositionedPlayableFile(positionedPreparedPlayableFile: PositionedPreparedPlayableFile): PositionedPlayableFile {
		val playableFile = positionedPreparedPlayableFile.preparedPlayableFile ?: throw IllegalStateException("Should not call this method with a null preparedPlayableFile")

		playableFile
			.bufferingPlaybackFile
			.promiseBufferedPlaybackFile()
			.then { beginQueueingPreparingPlayers() }
		return PositionedPlayableFile(
			playableFile.playbackHandler,
			playableFile.playableFileVolumeManager,
			positionedPreparedPlayableFile.positionedFile)
	}

	private fun preparePlayableFileAgainIfNecessary(positionedPreparedPlayableFile: PositionedPreparedPlayableFile): Promise<PositionedPreparedPlayableFile> {
		if (!positionedPreparedPlayableFile.isEmpty) return Promise(positionedPreparedPlayableFile)

		val positionedFile = positionedPreparedPlayableFile.positionedFile

		logger.warn("$positionedFile failed to prepare in time. Cancelling and preparing again.")
		currentPreparingPlaybackHandlerPromise?.preparedPlaybackFilePromise?.cancel()
		return PositionedPreparingFile(
			positionedFile,
			playbackPreparer.promisePreparedPlaybackFile(positionedFile.serviceFile, Duration.ZERO)).also {
				currentPreparingPlaybackHandlerPromise = it
		}.promisePositionedPreparedPlaybackFile()
	}

	private class PositionedPreparingFile(
		override val positionedFile: PositionedFile,
		override val preparedPlaybackFilePromise: Promise<PreparedPlayableFile>)
		: ProvidePreparedPlaybackFile {

		override fun promisePositionedPreparedPlaybackFile(): Promise<PositionedPreparedPlayableFile> =
			preparedPlaybackFilePromise.then(
				{ handler -> PositionedPreparedPlayableFile(positionedFile, handler) },
				{ error -> throw PreparationException(positionedFile, error) })
	}

	private class PositionedUnfaultingPreparingFile(
		override val positionedFile: PositionedFile,
		override val preparedPlaybackFilePromise: Promise<PreparedPlayableFile>) : ProvidePreparedPlaybackFile {

		override fun promisePositionedPreparedPlaybackFile(): Promise<PositionedPreparedPlayableFile> =
			preparedPlaybackFilePromise.then(
				{ handler -> PositionedPreparedPlayableFile(positionedFile, handler) },
				{ error ->
					logger.warn("An error occurred during preparation, returning an empty handler to trigger re-preparation", error)
					PositionedPreparedPlayableFile.emptyHandler(positionedFile)
				})
	}

	private interface ProvidePreparedPlaybackFile {
		val positionedFile: PositionedFile
		val preparedPlaybackFilePromise: Promise<PreparedPlayableFile>

		fun promisePositionedPreparedPlaybackFile(): Promise<PositionedPreparedPlayableFile>
	}
}
