package com.lasthopesoftware.bluewater.client.playback.file.fakes

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.buffering.BufferingPlaybackFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile

class FakePreparedPlayableFile<PlaybackHandler>(playbackHandler: PlaybackHandler) :
    PreparedPlayableFile(
        playbackHandler,
        NoTransformVolumeManager(),
        playbackHandler
    ) where PlaybackHandler : PlayableFile, PlaybackHandler : BufferingPlaybackFile
