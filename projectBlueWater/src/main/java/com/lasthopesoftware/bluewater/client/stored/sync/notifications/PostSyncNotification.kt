package com.lasthopesoftware.bluewater.client.stored.sync.notifications

interface PostSyncNotification {
	fun notify(notificationText: String?)
}
