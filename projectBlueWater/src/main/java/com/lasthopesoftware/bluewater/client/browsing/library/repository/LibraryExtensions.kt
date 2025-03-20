package com.lasthopesoftware.bluewater.client.browsing.library.repository

import kotlinx.serialization.json.Json

val Library.isReadPermissionsRequiredForLibrary: Boolean
	get() = isUsingExistingFiles || SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(parsedConnectionSettings()?.syncedFileLocation)

val StoredMediaCenterConnectionSettings.isWritePermissionsRequiredForLibrary: Boolean
	get() = SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(syncedFileLocation)

val Library.libraryId: LibraryId
	get() = LibraryId(id)

fun Library.parsedConnectionSettings(): StoredMediaCenterConnectionSettings? =
	connectionSettings?.let { Json.decodeFromString(it) }
