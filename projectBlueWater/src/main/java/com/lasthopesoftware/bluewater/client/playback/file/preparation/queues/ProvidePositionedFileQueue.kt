package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

interface ProvidePositionedFileQueue {
    fun provideQueue(libraryId: LibraryId, playlist: List<ServiceFile>, startingAt: Int): PositionedFileQueue
    val isRepeating: Boolean
}
