package com.lasthopesoftware.bluewater.client.browsing.library.request.write

import android.app.Notification
import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.request.IStoragePermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.request.StoragePermissionsRequestNotificationBuilder

/**
 * Created by david on 7/10/16.
 */
class StorageWritePermissionsRequestNotificationBuilder(private val storagePermissionsRequestNotificationBuilder: IStoragePermissionsRequestNotificationBuilder) :
    IStorageWritePermissionsRequestNotificationBuilder {
    constructor(context: Context?) : this(
        StoragePermissionsRequestNotificationBuilder(
            context!!
        )
    ) {
    }

    override fun buildWritePermissionsRequestNotification(libraryId: Int): Notification? {
        return storagePermissionsRequestNotificationBuilder.buildStoragePermissionsRequestNotification(
            libraryId
        )
    }
}
