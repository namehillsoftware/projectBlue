package com.lasthopesoftware.bluewater.client.stored.library.permissions.write

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

fun interface BroadcastWritePermissionsRequest {
    fun sendWritePermissionsNeededBroadcast(libraryId: LibraryId)
}
