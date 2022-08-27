package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

class CyclicalFileQueueProvider : IPositionedFileQueueProvider {

	override val isRepeating: Boolean = true

    override fun provideQueue(playlist: List<ServiceFile>, startingAt: Int): IPositionedFileQueue {
        val truncatedList = QueueSplicers.getTruncatedList(playlist, startingAt)
        val endingPosition = playlist.size - truncatedList.size
        for (i in 0 until endingPosition) truncatedList.add(PositionedFile(i, playlist[i]))
        return RepeatingPositionedFileQueue(truncatedList)
    }
}
