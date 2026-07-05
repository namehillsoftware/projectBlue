package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileDuration
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class DurationFallbackPlayingFile(
    private val playingFile: PlayingFile,
    private val fallbackDurationReader: ReadFileDuration
) : PlayingFile by playingFile {
	override val duration: Promise<Duration>
		get() = Promise.whenAny(playingFile.duration, fallbackDurationReader.duration)
}
