package com.lasthopesoftware.bluewater.client.playback.engine.events

fun interface OnPlaylistError {
	fun onError(error: Throwable)
}
