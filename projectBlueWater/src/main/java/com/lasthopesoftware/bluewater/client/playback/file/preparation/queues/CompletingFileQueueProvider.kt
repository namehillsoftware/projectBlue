package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile

class CompletingFileQueueProvider : IPositionedFileQueueProvider {
	override val isRepeating: Boolean = false

	override fun provideQueue(playlist: List<ServiceFile>, startingAt: Int): IPositionedFileQueue {
		return CompletingPositionedFileQueue(QueueSplicers.getTruncatedList(playlist, startingAt))
	}
}
