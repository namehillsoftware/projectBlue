package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.launchMusicService
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class LaunchPlaybackFromResult(private val context: Context) : ImmediateResponse<String?, Unit> {
    override fun respond(result: String?) = launchMusicService(context, result)
}
