package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import java.util.ArrayDeque

internal class CompletingPositionedFileQueue(playlist: List<PositionedFile>) :
    IPositionedFileQueue {
    private val playlist by lazy { ArrayDeque(playlist) }

    override fun poll(): PositionedFile? {
        return playlist.poll()
    }

    override fun peek(): PositionedFile? {
        return playlist.peek()
    }
}
