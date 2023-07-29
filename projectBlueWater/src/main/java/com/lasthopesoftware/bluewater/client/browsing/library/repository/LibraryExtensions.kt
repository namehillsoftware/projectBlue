package com.lasthopesoftware.bluewater.client.browsing.library.repository

val Library.isReadPermissionsRequiredForLibrary: Boolean
	get() = isUsingExistingFiles || Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(syncedFileLocation)

val Library.isWritePermissionsRequiredForLibrary: Boolean
	get() = Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(syncedFileLocation)
