package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile

internal class PositionedPreparedPlayableFile(
    val positionedFile: PositionedFile,
    val preparedPlayableFile: PreparedPlayableFile?
) {
    val isEmpty: Boolean
        get() = preparedPlayableFile == null

    companion object {
        fun emptyHandler(positionedFile: PositionedFile): PositionedPreparedPlayableFile {
            return PositionedPreparedPlayableFile(positionedFile, null)
        }
    }
}
