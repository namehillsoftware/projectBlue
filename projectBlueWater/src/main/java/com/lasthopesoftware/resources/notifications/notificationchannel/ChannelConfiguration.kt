package com.lasthopesoftware.resources.notifications.notificationchannel

interface ChannelConfiguration {
	val channelId: String
	val channelName: String
	val channelDescription: String
	val channelImportance: Int
}
