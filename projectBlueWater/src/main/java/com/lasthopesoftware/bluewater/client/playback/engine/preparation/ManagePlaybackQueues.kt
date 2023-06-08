package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.PositionedFileQueue
import java.io.Closeable

interface ManagePlaybackQueues : Closeable {
    fun initializePreparedPlaybackQueue(positionedFileQueue: PositionedFileQueue): PreparedPlayableFileQueue
    fun tryUpdateQueue(positionedFileQueue: PositionedFileQueue): Boolean
}
