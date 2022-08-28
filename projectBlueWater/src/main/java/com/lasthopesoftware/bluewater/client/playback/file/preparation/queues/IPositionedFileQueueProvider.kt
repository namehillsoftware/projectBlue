package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile

interface IPositionedFileQueueProvider {
    fun provideQueue(playlist: List<ServiceFile>, startingAt: Int): IPositionedFileQueue
    val isRepeating: Boolean
}
