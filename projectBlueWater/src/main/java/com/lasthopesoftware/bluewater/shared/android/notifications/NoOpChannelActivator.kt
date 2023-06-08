package com.lasthopesoftware.bluewater.shared.android.notifications

import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.ActivateChannel
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.ChannelConfiguration

object NoOpChannelActivator : ActivateChannel {
	override fun activateChannel(channelConfiguration: ChannelConfiguration): String {
		return channelConfiguration.channelId
	}
}
