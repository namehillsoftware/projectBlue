package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

class PlaybackStartedBroadcaster(private val sendApplicationMessages: SendApplicationMessages) {
    fun broadcastPlaybackStarted() = sendApplicationMessages.sendMessage(PlaybackMessage.PlaybackStarted)
}
