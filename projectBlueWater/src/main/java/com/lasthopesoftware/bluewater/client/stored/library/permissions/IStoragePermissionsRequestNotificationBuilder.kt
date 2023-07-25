package com.lasthopesoftware.bluewater.client.stored.library.permissions

import android.app.Notification

/**
 * Created by david on 7/10/16.
 */
interface IStoragePermissionsRequestNotificationBuilder {
    fun buildStoragePermissionsRequestNotification(libraryId: Int): Notification?
}
