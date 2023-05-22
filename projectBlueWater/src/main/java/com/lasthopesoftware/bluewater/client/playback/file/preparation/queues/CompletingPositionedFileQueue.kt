package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import java.util.ArrayDeque

internal class CompletingPositionedFileQueue(override val libraryId: LibraryId, playlist: List<PositionedFile>) : PositionedFileQueue {
    private val playlist by lazy { ArrayDeque(playlist) }

    override fun poll(): PositionedFile? {
        return playlist.poll()
    }

    override fun peek(): PositionedFile? {
        return playlist.peek()
    }
}
