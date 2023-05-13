package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress
import com.namehillsoftware.handoff.promises.Promise
import java.io.Closeable

interface PlayableFile : ReadFileProgress, Closeable {
    fun promisePlayback(): Promise<PlayingFile>
}
