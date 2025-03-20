package com.lasthopesoftware.bluewater.client.browsing.library.repository

import androidx.annotation.Keep

@Keep
enum class SyncedFileLocation {
	EXTERNAL, INTERNAL;

	companion object {
		val ExternalDiskAccessSyncLocations: Set<SyncedFileLocation> = setOf(EXTERNAL)
	}
}
