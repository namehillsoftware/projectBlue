package com.lasthopesoftware.bluewater.client.browsing.library.repository

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.IdentifiableEntity

@Keep
data class Library(
	override var id: Int = -1,
	var libraryName: String? = null,
	var accessCode: String? = null,
	var userName: String? = null,
	var password: String? = null,
	var sslCertificateUri: String? = null,
	var isLocalOnly: Boolean = false,
	var isRepeating: Boolean = false,
	var nowPlayingId: Int = -1,
	var nowPlayingProgress: Long = -1,
	var selectedViewType: ViewType? = null,
	var selectedView: Int = -1,
	var savedTracksString: String? = null,
	var syncedFileLocation: SyncedFileLocation? = null,
	var isUsingExistingFiles: Boolean = false,
	var isSyncLocalConnectionsOnly: Boolean = false,
	var isWakeOnLanEnabled: Boolean = false,
	var sslCertificate: String? = null) : IdentifiableEntity {

	@Keep
	enum class SyncedFileLocation {
		EXTERNAL, INTERNAL;

		companion object {
			val ExternalDiskAccessSyncLocations: Set<SyncedFileLocation> = setOf(EXTERNAL)
		}
	}

	@Keep
	enum class ViewType {
		StandardServerView, PlaylistView, DownloadView, SearchView
	}
}
