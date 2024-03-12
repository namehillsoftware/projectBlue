package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.PositionedFileQueue
import com.lasthopesoftware.resources.closables.ResettableCloseable

class PreparedPlaybackQueueResourceManagement(
    private val playbackPreparerProvider: IPlayableFilePreparationSourceProvider,
    private val preparedPlaybackQueueConfiguration: IPreparedPlaybackQueueConfiguration
) : ManagePlaybackQueues, ResettableCloseable {
    private var preparedPlaybackQueue: PreparedPlayableFileQueue? = null

    override fun initializePreparedPlaybackQueue(positionedFileQueue: PositionedFileQueue): PreparedPlayableFileQueue {
        reset()
        return PreparedPlayableFileQueue(
            preparedPlaybackQueueConfiguration,
            playbackPreparerProvider.providePlayableFilePreparationSource(),
            positionedFileQueue
        ).also { preparedPlaybackQueue = it }
    }

    override fun tryUpdateQueue(positionedFileQueue: PositionedFileQueue): Boolean {
		return preparedPlaybackQueue?.updateQueue(positionedFileQueue) != null
    }

	override fun reset() {
		preparedPlaybackQueue?.close()
	}

	override fun close() {
        reset()
    }
}
