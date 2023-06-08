package com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel

interface ActivateChannel {
    fun activateChannel(channelConfiguration: ChannelConfiguration): String
}
