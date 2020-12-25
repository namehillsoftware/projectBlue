package com.lasthopesoftware.bluewater.client.playback.file.progress

interface NotifyFilePlaybackError<T : Exception?> {
	fun playbackError(onError: ((T) -> Unit)?)
}
