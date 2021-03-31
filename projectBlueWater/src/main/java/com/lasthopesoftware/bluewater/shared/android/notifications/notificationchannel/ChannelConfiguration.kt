package com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel

interface ChannelConfiguration {
	val channelId: String
	val channelName: String
	val channelDescription: String
	val channelImportance: Int
}
