package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileDuration
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class DurationFallbackPlayingFile(
    private val playingFile: PlayingFile,
    private val fallbackDurationReader: ReadFileDuration
) : PlayingFile by playingFile {
	override val duration: Promise<Duration>
		get() = playingFile.duration.cancelBackEventually { duration ->
			duration.takeIf { it.isLongerThan(Duration.ZERO) }?.toPromise() ?: fallbackDurationReader.duration
		}
}
