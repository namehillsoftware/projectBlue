package com.lasthopesoftware.bluewater.client.browsing.library.request

import android.app.Notification

/**
 * Created by david on 7/10/16.
 */
interface IStoragePermissionsRequestNotificationBuilder {
    fun buildStoragePermissionsRequestNotification(libraryId: Int): Notification?
}
