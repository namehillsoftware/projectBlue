package com.lasthopesoftware.bluewater.client.stored.service.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.ChannelConfiguration

class SyncChannelProperties(private val context: Context) : ChannelConfiguration {
	override val channelId: String
		get() = Companion.channelId

	override val channelName by lazy { context.getString(R.string.app_name) + " sync" }

	override val channelDescription by lazy { String.format("Notifications for %1\$s", channelName) }

	override val channelImportance: Int
		get() = Companion.channelImportance

	companion object {
		private const val channelId = "ProjectBlueSync"
		private val channelImportance =
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) 1
			else NotificationManager.IMPORTANCE_MIN
	}

}
