package com.lasthopesoftware.bluewater.client.stored.library.permissions.write

import android.app.Notification
import com.lasthopesoftware.bluewater.client.stored.library.permissions.IStoragePermissionsRequestNotificationBuilder

/**
 * Created by david on 7/10/16.
 */
class StorageWritePermissionsRequestNotificationBuilder(private val storagePermissionsRequestNotificationBuilder: IStoragePermissionsRequestNotificationBuilder) :
	IStorageWritePermissionsRequestNotificationBuilder {

    override fun buildWritePermissionsRequestNotification(libraryId: Int): Notification? {
        return storagePermissionsRequestNotificationBuilder.buildStoragePermissionsRequestNotification(
            libraryId
        )
    }
}
