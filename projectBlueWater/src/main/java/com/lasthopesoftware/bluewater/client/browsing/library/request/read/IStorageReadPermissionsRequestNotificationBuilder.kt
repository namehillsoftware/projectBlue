package com.lasthopesoftware.bluewater.client.browsing.library.request.read

import android.app.Notification

/**
 * Created by david on 7/3/16.
 */
interface IStorageReadPermissionsRequestNotificationBuilder {
    fun buildReadPermissionsRequestNotification(libraryId: Int): Notification?
}
