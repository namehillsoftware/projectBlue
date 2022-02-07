package com.lasthopesoftware.bluewater.client.playback.service

import android.content.Context
import com.namehillsoftware.handoff.promises.Promise

class PlaybackServiceController(private val context: Context) : ControlPlaybackService {
	override fun promiseIsMarkedForPlay(): Promise<Boolean> = PlaybackService.promiseIsMarkedForPlay(context)

	override fun setRepeating() = PlaybackService.setRepeating(context)

	override fun setCompleting() = PlaybackService.setCompleting(context)

	override fun play() = PlaybackService.play(context)

	override fun pause() = PlaybackService.play(context)
}
