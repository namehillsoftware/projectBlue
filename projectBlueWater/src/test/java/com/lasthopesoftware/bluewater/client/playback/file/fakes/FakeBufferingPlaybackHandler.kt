package com.lasthopesoftware.bluewater.client.playback.file.fakes

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

open class FakeBufferingPlaybackHandler : IBufferingPlaybackFile, PlayableFile, PlayingFile, PlayedFile {
	var isPlaying = false
		private set

	protected var backingCurrentPosition = 0

	fun setCurrentPosition(position: Int) {
		backingCurrentPosition = position
	}

	override fun promisePlayback(): Promise<PlayingFile> {
		isPlaying = true
		return Promise(this)
	}

	override fun close() {}
	override fun promiseBufferedPlaybackFile(): Promise<IBufferingPlaybackFile> {
		return Promise(this)
	}

	override val progress: Promise<Duration>
		get() = Duration.millis(backingCurrentPosition.toLong()).toPromise()

	override fun promisePause(): Promise<PlayableFile> {
		isPlaying = false
		return Promise(this)
	}

	override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> {
		return object : ProgressingPromise<Duration, PlayedFile>() {
			override val progress: Promise<Duration>
				get() = Duration.millis(backingCurrentPosition.toLong()).toPromise()

			init {
				resolve(this@FakeBufferingPlaybackHandler)
			}
		}
	}

	override val duration: Promise<Duration>
		get() = Duration.millis(backingCurrentPosition.toLong()).toPromise()
}
