package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.PositionedFileQueue

class PreparedPlaybackQueueResourceManagement(
    private val playbackPreparerProvider: IPlayableFilePreparationSourceProvider,
    private val preparedPlaybackQueueConfiguration: IPreparedPlaybackQueueConfiguration
) : ManagePlaybackQueues {
    private var preparedPlaybackQueue: PreparedPlayableFileQueue? = null

    override fun initializePreparedPlaybackQueue(positionedFileQueue: PositionedFileQueue): PreparedPlayableFileQueue {
        close()
        return PreparedPlayableFileQueue(
            preparedPlaybackQueueConfiguration,
            playbackPreparerProvider.providePlayableFilePreparationSource(),
            positionedFileQueue
        ).also { preparedPlaybackQueue = it }
    }

    override fun tryUpdateQueue(positionedFileQueue: PositionedFileQueue): Boolean {
		return preparedPlaybackQueue?.updateQueue(positionedFileQueue)?.let { true } ?: false
    }

    override fun close() {
        preparedPlaybackQueue?.close()
    }
}
