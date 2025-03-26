package com.lasthopesoftware.bluewater.client.browsing.library.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation

val LibrarySettings.isReadPermissionsRequired: Boolean
	get() = isUsingExistingFiles || SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(syncedFileLocation)

val LibrarySettings.isWritePermissionsRequired: Boolean
	get() = SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(syncedFileLocation)
