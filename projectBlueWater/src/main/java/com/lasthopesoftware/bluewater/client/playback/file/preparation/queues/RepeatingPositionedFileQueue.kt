package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import java.util.ArrayDeque

internal class RepeatingPositionedFileQueue(override val libraryId: LibraryId, playlist: List<PositionedFile>) :
    PositionedFileQueue {
    private val playlist by lazy { ArrayDeque(playlist) }

    override fun poll(): PositionedFile? {
        val positionedFile = playlist.poll() ?: return null
        playlist.offer(positionedFile)
        return positionedFile
    }

    override fun peek(): PositionedFile? = playlist.peek()
}
