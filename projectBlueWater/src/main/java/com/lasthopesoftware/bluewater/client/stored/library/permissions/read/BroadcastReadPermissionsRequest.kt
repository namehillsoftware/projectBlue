package com.lasthopesoftware.bluewater.client.stored.library.permissions.read

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

fun interface BroadcastReadPermissionsRequest {
    fun sendReadPermissionsRequestedBroadcast(libraryId: LibraryId)
}
