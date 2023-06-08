package com.lasthopesoftware.bluewater.client.browsing.library.request.read

import android.app.Notification
import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.request.IStoragePermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.request.StoragePermissionsRequestNotificationBuilder

/**
 * Created by david on 7/3/16.
 */
class StorageReadPermissionsRequestNotificationBuilder(private val storagePermissionsRequestNotificationBuilder: IStoragePermissionsRequestNotificationBuilder) :
    IStorageReadPermissionsRequestNotificationBuilder {
    constructor(context: Context?) : this(
        StoragePermissionsRequestNotificationBuilder(
            context!!
        )
    ) {
    }

    override fun buildReadPermissionsRequestNotification(libraryId: Int): Notification? {
        return storagePermissionsRequestNotificationBuilder.buildStoragePermissionsRequestNotification(
            libraryId
        )
    }
}
