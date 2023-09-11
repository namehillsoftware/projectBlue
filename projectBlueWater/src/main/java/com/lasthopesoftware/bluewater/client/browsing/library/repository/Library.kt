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
	var sslCertificateFingerprint: ByteArray = ByteArray(0)
) : IdentifiableEntity {

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

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Library

		if (id != other.id) return false
		if (libraryName != other.libraryName) return false
		if (accessCode != other.accessCode) return false
		if (userName != other.userName) return false
		if (password != other.password) return false
		if (isLocalOnly != other.isLocalOnly) return false
		if (isRepeating != other.isRepeating) return false
		if (nowPlayingId != other.nowPlayingId) return false
		if (nowPlayingProgress != other.nowPlayingProgress) return false
		if (selectedViewType != other.selectedViewType) return false
		if (selectedView != other.selectedView) return false
		if (savedTracksString != other.savedTracksString) return false
		if (syncedFileLocation != other.syncedFileLocation) return false
		if (isUsingExistingFiles != other.isUsingExistingFiles) return false
		if (isSyncLocalConnectionsOnly != other.isSyncLocalConnectionsOnly) return false
		if (isWakeOnLanEnabled != other.isWakeOnLanEnabled) return false
		if (!sslCertificateFingerprint.contentEquals(other.sslCertificateFingerprint)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id
		result = 31 * result + (libraryName?.hashCode() ?: 0)
		result = 31 * result + (accessCode?.hashCode() ?: 0)
		result = 31 * result + (userName?.hashCode() ?: 0)
		result = 31 * result + (password?.hashCode() ?: 0)
		result = 31 * result + isLocalOnly.hashCode()
		result = 31 * result + isRepeating.hashCode()
		result = 31 * result + nowPlayingId
		result = 31 * result + nowPlayingProgress.hashCode()
		result = 31 * result + (selectedViewType?.hashCode() ?: 0)
		result = 31 * result + selectedView
		result = 31 * result + (savedTracksString?.hashCode() ?: 0)
		result = 31 * result + (syncedFileLocation?.hashCode() ?: 0)
		result = 31 * result + isUsingExistingFiles.hashCode()
		result = 31 * result + isSyncLocalConnectionsOnly.hashCode()
		result = 31 * result + isWakeOnLanEnabled.hashCode()
		result = 31 * result + (sslCertificateFingerprint.contentHashCode())
		return result
	}
}
