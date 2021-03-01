package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress

import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class ExoPlayerFileProgressReader(private val exoPlayer: PromisingExoPlayer) : ReadFileProgress {
	private var currentDurationPromise: Promise<Duration> = Promise.empty()
	private var fileProgress = Duration.ZERO

	@get:Synchronized
	override val progress: Promise<Duration>
		get() =
			currentDurationPromise
				.eventually({ promiseProgress() }, { promiseProgress() })
				.also { currentDurationPromise = it }


	private fun promiseProgress(): Promise<Duration> =
		exoPlayer.getPlayWhenReady().eventually { isPlaying ->
			if (!isPlaying) fileProgress.toPromise()
			else {
				exoPlayer.getCurrentPosition().then { currentPosition ->
					if (currentPosition != fileProgress.millis)
						Duration.millis(currentPosition).also { fileProgress = it }
					else fileProgress
				}
			}
		}
}
