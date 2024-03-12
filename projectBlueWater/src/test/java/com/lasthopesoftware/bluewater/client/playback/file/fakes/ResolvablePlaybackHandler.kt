package com.lasthopesoftware.bluewater.client.playback.file.fakes

import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException
import com.lasthopesoftware.promises.extensions.ProgressedPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import kotlin.coroutines.cancellation.CancellationException

class ResolvablePlaybackHandler : FakeBufferingPlaybackHandler() {
	private val promise: ProgressedPromise<Duration, PlayedFile> = object : ProgressedPromise<Duration, PlayedFile>(MessengerOperator { messenger -> this.messenger = messenger }) {
		override val progress: Promise<Duration>
			get() = Duration.millis(backingCurrentPosition.toLong()).toPromise()
	}

	private var messenger: Messenger<PlayedFile>? = null

	override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> = promise

	override fun close() {
		super.close()
		messenger?.sendRejection(CancellationException())
	}

	fun resolve() {
		if (!isClosed) messenger?.sendResolution(this)
		else messenger?.sendRejection(CancellationException())
		messenger = null
	}

	fun reject(error: Throwable) {
		messenger?.sendRejection(PlaybackException(this, error))
	}
}
