package com.lasthopesoftware.bluewater.client.stored.library.permissions

import android.app.Notification
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

/**
 * Created by david on 7/10/16.
 */
interface IStoragePermissionsRequestNotificationBuilder {
    fun buildStoragePermissionsRequestNotification(libraryId: LibraryId): Notification?
}
