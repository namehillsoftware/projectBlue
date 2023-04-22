package com.lasthopesoftware.bluewater.client.playback.file.preparation

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume

open class PreparedPlayableFile(
    val playbackHandler: PlayableFile,
    val playableFileVolumeManager: ManagePlayableFileVolume,
    val bufferingPlaybackFile: IBufferingPlaybackFile
)
