package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileDuration
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

interface PlayingFile : ReadFileDuration {
    fun promisePause(): Promise<PlayableFile>
    fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile>
}
