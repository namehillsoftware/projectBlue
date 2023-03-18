package com.lasthopesoftware.bluewater.client.browsing.library.request

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsPendingIntentBuilder
import com.lasthopesoftware.bluewater.client.settings.IEditClientSettingsPendingIntentBuilder
import com.lasthopesoftware.resources.strings.IStringResourceProvider
import com.lasthopesoftware.resources.strings.StringResourceProvider

/**
 * Created by david on 7/10/16.
 */
class StoragePermissionsRequestNotificationBuilder(
    private val notificationBuilder: NotificationCompat.Builder,
    private val stringResourceProvider: IStringResourceProvider,
    private val editServerSettingsPendingIntentBuilder: IEditClientSettingsPendingIntentBuilder
) : IStoragePermissionsRequestNotificationBuilder {
    constructor(context: Context) : this(
        NotificationCompat.Builder(context),
		StringResourceProvider(context),
		EditClientSettingsPendingIntentBuilder(context)
    )

	override fun buildStoragePermissionsRequestNotification(libraryId: Int): Notification {
        notificationBuilder.setSmallIcon(R.drawable.now_playing_status_icon_white)
        notificationBuilder.setContentTitle(stringResourceProvider.getString(R.string.permissions_needed))
        notificationBuilder.setContentText(stringResourceProvider.getString(R.string.permissions_needed_launch_settings))
        notificationBuilder.setContentIntent(
            editServerSettingsPendingIntentBuilder.buildEditServerSettingsPendingIntent(
                libraryId
            )
        )
        notificationBuilder.setAutoCancel(true)
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return notificationBuilder.build()
    }
}
