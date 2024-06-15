package com.lasthopesoftware.bluewater.client.stored.library.permissions

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.ChannelConfiguration
import com.lasthopesoftware.resources.strings.GetStringResources

class StoragePermissionsRequestNotificationBuilder(
    private val context: Context,
    private val stringResources: GetStringResources,
    private val intentBuilder: BuildIntents,
    private val channelProperties: ChannelConfiguration,
) : IStoragePermissionsRequestNotificationBuilder {

	override fun buildStoragePermissionsRequestNotification(libraryId: Int): Notification {
		val notificationBuilder = NotificationCompat.Builder(context, channelProperties.channelId)

		notificationBuilder.setSmallIcon(R.drawable.now_playing_status_icon_white)
		notificationBuilder.setContentTitle(stringResources.permissionsNeeded)
		notificationBuilder.setContentText(stringResources.permissionsNeededLaunchSettings)
		notificationBuilder.setContentIntent(
			intentBuilder.buildLibraryServerSettingsPendingIntent(LibraryId(libraryId))
		)
		notificationBuilder.setAutoCancel(true)
		notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
		return notificationBuilder.build()
	}
}
