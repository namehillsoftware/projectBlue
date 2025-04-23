package com.lasthopesoftware.bluewater.client.playback.file.fakes

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.buffering.BufferingPlaybackFile
import com.lasthopesoftware.promises.extensions.ProgressedPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue

open class FakeBufferingPlaybackHandler : BufferingPlaybackFile, PlayableFile, PlayingFile, PlayedFile {
	private val playingStates = ConcurrentLinkedQueue<Boolean>().apply { add(false) }

	val recordedPlayingStates
		get() = playingStates.toList()

	val isPlaying
		get() = playingStates.last()

	var isClosed = false
		private set

	protected var backingCurrentPosition = 0

	fun setCurrentPosition(position: Int) {
		backingCurrentPosition = position
	}

	override fun promisePlayback(): Promise<PlayingFile> {
		playingStates.add(true)
		return Promise(this)
	}

	override fun close() {
		isClosed = true
	}

	override fun promiseBufferedPlaybackFile(): Promise<BufferingPlaybackFile> {
		return Promise(this)
	}

	override val progress: Promise<Duration>
		get() = Duration.millis(backingCurrentPosition.toLong()).toPromise()

	override fun promisePause(): Promise<PlayableFile> {
		playingStates.add(false)
		return Promise(this)
	}

	override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> {
		return object : ProgressedPromise<Duration, PlayedFile>() {
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
