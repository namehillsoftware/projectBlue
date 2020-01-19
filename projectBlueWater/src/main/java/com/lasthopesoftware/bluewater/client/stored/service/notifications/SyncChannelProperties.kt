package com.lasthopesoftware.bluewater.client.stored.service.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration
import com.namehillsoftware.lazyj.Lazy

class SyncChannelProperties(private val context: Context) : ChannelConfiguration {
	private val lazyChannelName = Lazy<String> { context.getString(R.string.app_name) + " sync" }
	private val lazyChannelDescription = Lazy<String> { String.format("Notifications for %1\$s", lazyChannelName.getObject()) }

	override val channelId: String
		get() {	return Companion.channelId }

	override val channelName: String
		get() { return lazyChannelName.getObject()	}

	override val channelDescription: String
		get() {	return lazyChannelDescription.getObject() }

	override val channelImportance: Int
		get() {	return Companion.channelImportance }

	companion object {
		private const val channelId = "MusicCanoeSync"
		private val channelImportance = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) 1 else NotificationManager.IMPORTANCE_MIN
	}

}
