package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

class CyclicalFileQueueProvider : ProvidePositionedFileQueue {

	override val isRepeating: Boolean = true

    override fun provideQueue(playlist: List<ServiceFile>, startingAt: Int): PositionedFileQueue {
        val truncatedList = QueueSplicers.getTruncatedList(playlist, startingAt)
        val endingPosition = playlist.size - truncatedList.size
        for (i in 0 until endingPosition) truncatedList.add(PositionedFile(i, playlist[i]))
        return RepeatingPositionedFileQueue(truncatedList)
    }
}
