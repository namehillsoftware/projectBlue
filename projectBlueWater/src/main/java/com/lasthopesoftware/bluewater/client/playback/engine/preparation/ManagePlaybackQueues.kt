package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.PositionedFileQueue

interface ManagePlaybackQueues {
    fun initializePreparedPlaybackQueue(positionedFileQueue: PositionedFileQueue): PreparedPlayableFileQueue
    fun tryUpdateQueue(positionedFileQueue: PositionedFileQueue): Boolean
	fun reset()
}
