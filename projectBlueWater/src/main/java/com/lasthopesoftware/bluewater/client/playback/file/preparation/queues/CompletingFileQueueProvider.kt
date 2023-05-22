package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

class CompletingFileQueueProvider : ProvidePositionedFileQueue {
	override val isRepeating: Boolean = false

	override fun provideQueue(libraryId: LibraryId, playlist: List<ServiceFile>, startingAt: Int): PositionedFileQueue {
		return CompletingPositionedFileQueue(libraryId, QueueSplicers.getTruncatedList(playlist, startingAt))
	}
}
