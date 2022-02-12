package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import android.content.Intent
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

class PlaybackStartedBroadcaster(private val sendMessages: SendMessages) {
    fun broadcastPlaybackStarted() {
        val playbackBroadcastIntent = Intent(PlaylistEvents.onPlaylistStart)
        sendMessages.sendBroadcast(playbackBroadcastIntent)
    }
}
