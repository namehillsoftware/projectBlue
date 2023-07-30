package com.lasthopesoftware.bluewater.client.stored.library.permissions.write

import android.app.Notification

/**
 * Created by david on 7/3/16.
 */
interface IStorageWritePermissionsRequestNotificationBuilder {
    fun buildWritePermissionsRequestNotification(libraryId: Int): Notification?
}
