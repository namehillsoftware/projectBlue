package com.lasthopesoftware.bluewater.client.playback.file

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class EmptyPlaybackHandler(private val fileDuration: Int) : ProgressedPromise<Duration, PlayedFile>(), IBufferingPlaybackFile, PlayableFile, PlayingFile, PlayedFile {
	private val promisedThis: Promise<*> = this

	init {
		resolve(this)
	}

	override val progress: Promise<Duration>
		get() = Duration.ZERO.toPromise()

	override val duration: Promise<Duration>
		get() = Duration.millis(fileDuration.toLong()).toPromise()

	@Suppress("UNCHECKED_CAST")
	override fun promisePlayback(): Promise<PlayingFile> {
		return promisedThis as Promise<PlayingFile>
	}

	@Suppress("UNCHECKED_CAST")
	override fun promiseBufferedPlaybackFile(): Promise<IBufferingPlaybackFile> {
		return promisedThis as Promise<IBufferingPlaybackFile>
	}

	override fun promisePause(): Promise<PlayableFile> {
		return promisedThis as Promise<PlayableFile>
	}

	override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> {
		return this
	}

	override fun close() {}
}
