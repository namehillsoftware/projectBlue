package com.lasthopesoftware.bluewater.client.playback.service

import android.content.Context
import com.namehillsoftware.handoff.promises.Promise

class PlaybackStateLookup(private val context: Context) : GetPlaybackState {
	override fun promiseIsMarkedForPlay(): Promise<Boolean> = PlaybackService.promiseIsMarkedForPlay(context)
}
