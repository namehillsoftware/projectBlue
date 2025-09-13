package com.lasthopesoftware.bluewater.client.stored.library.permissions.read

import android.app.Notification
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

/**
 * Created by david on 7/3/16.
 */
interface IStorageReadPermissionsRequestNotificationBuilder {
    fun buildReadPermissionsRequestNotification(libraryId: LibraryId): Notification?
}
