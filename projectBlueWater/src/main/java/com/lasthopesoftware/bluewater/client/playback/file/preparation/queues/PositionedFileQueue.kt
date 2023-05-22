package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

interface PositionedFileQueue {

	val libraryId: LibraryId

    fun poll(): PositionedFile?
    fun peek(): PositionedFile?
}
