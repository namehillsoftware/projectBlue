package com.lasthopesoftware.resources.notifications

import com.lasthopesoftware.resources.notifications.notificationchannel.ActivateChannel
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration

class NoOpChannelActivator : ActivateChannel {
	override fun activateChannel(channelConfiguration: ChannelConfiguration): String {
		return channelConfiguration.channelId
	}
}
