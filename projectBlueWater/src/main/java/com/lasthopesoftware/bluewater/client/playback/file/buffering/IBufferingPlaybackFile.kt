package com.lasthopesoftware.bluewater.client.playback.file.buffering

import com.namehillsoftware.handoff.promises.Promise

interface IBufferingPlaybackFile {
    fun promiseBufferedPlaybackFile(): Promise<IBufferingPlaybackFile>
}
