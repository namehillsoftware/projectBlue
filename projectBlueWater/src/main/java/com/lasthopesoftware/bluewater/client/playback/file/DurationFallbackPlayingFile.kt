package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileDuration
import com.lasthopesoftware.promises.ResolvedPromiseBox
import com.lasthopesoftware.promises.UnkeptPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.concurrent.atomic.AtomicReference

class DurationFallbackPlayingFile(
    private val playingFile: PlayingFile,
    private val fallbackDurationReader: ReadFileDuration
) : PlayingFile by playingFile {

	private val currentDurationPromise = AtomicReference(ResolvedPromiseBox(UnkeptPromise.instance<Duration>()))

	override val duration: Promise<Duration>
		get() =
			currentDurationPromise.get().resolvedPromise ?: currentDurationPromise.updateAndGet { prev ->
				prev.run {
					if (resolvedPromise != null) this
					else {
						originalPromise.cancel()
						ResolvedPromiseBox(playingFile.duration)
					}
				}
			}.resolvedPromise ?: fallbackDurationReader.duration
}
