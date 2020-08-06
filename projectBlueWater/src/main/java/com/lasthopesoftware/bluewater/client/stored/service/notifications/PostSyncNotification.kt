package com.lasthopesoftware.bluewater.client.stored.service.notifications

interface PostSyncNotification {
	fun notify(notificationText: String?)
}
