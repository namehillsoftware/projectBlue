package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService.Companion.launchMusicService
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class LaunchPlaybackFromResult(private val context: Context, private val libraryId: LibraryId) : ImmediateResponse<String, Unit> {
    override fun respond(result: String) = launchMusicService(context, libraryId, result)
}
