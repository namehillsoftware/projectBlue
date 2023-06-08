package com.lasthopesoftware.bluewater.client.playback.file.volume

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.volume.IVolumeManagement

interface IPlaybackHandlerVolumeControllerFactory {
    fun manageVolume(
        positionedPlayableFile: PositionedPlayableFile?,
        initialHandlerVolume: Float
    ): IVolumeManagement?
}
