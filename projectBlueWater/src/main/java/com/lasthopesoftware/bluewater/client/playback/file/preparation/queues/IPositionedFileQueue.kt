package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

/**
 * Created by david on 11/16/16.
 */
interface IPositionedFileQueue {
    fun poll(): PositionedFile?
    fun peek(): PositionedFile?
}
