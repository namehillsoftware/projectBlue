package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

internal object QueueSplicers {
    fun getTruncatedList(playlist: List<ServiceFile>, startingAt: Int): MutableList<PositionedFile> {
        val positionedFiles: MutableList<PositionedFile> = ArrayList(playlist.size)
        for (i in startingAt until playlist.size) positionedFiles.add(
            PositionedFile(
                i,
                playlist[i]
            )
        )
        return positionedFiles
    }
}
