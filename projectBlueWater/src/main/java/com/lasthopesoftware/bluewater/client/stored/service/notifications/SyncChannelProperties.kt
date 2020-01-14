package com.lasthopesoftware.bluewater.client.stored.service.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold

class SyncChannelProperties(private val context: Context) : ChannelConfiguration {
	private val lazyChannelName: CreateAndHold<String> = object : AbstractSynchronousLazy<String>() {
		override fun create(): String {
			return context.getString(R.string.app_name)
		}
	}
	private val lazyChannelDescription: CreateAndHold<String> = object : AbstractSynchronousLazy<String>() {
		override fun create(): String {
			return String.format("Sync notifications for %1\$s", lazyChannelName.getObject())
		}
	}

	override fun getChannelId(): String {
		return Companion.channelId
	}

	override fun getChannelName(): String {
		return lazyChannelName.getObject()
	}

	override fun getChannelDescription(): String {
		return lazyChannelDescription.getObject()
	}

	override fun getChannelImportance(): Int {
		return Companion.channelImportance
	}

	companion object {
		private const val channelId = "MusicCanoeSync"
		private val channelImportance = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) 2 else NotificationManager.IMPORTANCE_LOW
	}

}
