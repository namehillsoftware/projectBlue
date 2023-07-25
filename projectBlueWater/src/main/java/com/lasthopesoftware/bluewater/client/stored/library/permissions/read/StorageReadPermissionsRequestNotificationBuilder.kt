package com.lasthopesoftware.bluewater.client.stored.library.permissions.read

import android.app.Notification
import com.lasthopesoftware.bluewater.client.stored.library.permissions.IStoragePermissionsRequestNotificationBuilder

/**
 * Created by david on 7/3/16.
 */
class StorageReadPermissionsRequestNotificationBuilder(
	private val storagePermissionsRequestNotificationBuilder: IStoragePermissionsRequestNotificationBuilder
) :
	IStorageReadPermissionsRequestNotificationBuilder {

	override fun buildReadPermissionsRequestNotification(libraryId: Int): Notification? {
        return storagePermissionsRequestNotificationBuilder.buildStoragePermissionsRequestNotification(libraryId)
    }
}
