package com.lasthopesoftware.bluewater.client.browsing.library.repository

import androidx.annotation.Keep
import java.util.*

@Keep
data class Library(
	private var _id: Int = -1,
	private var _libraryName: String? = null,
	private var _accessCode: String? = null,
	private var _userName: String? = null,
	private var _password: String? = null,
	private var _isLocalOnly: Boolean = false,
	private var _isRepeating: Boolean = false,
	private var _nowPlayingId: Int = -1,
	private var _nowPlayingProgress: Long = -1,
	private var _selectedViewType: ViewType? = null,
	private var _selectedView: Int = -1,
	private var _savedTracksString: String? = null,
	private var _customSyncedFilesPath: String? = null,
	private var _syncedFileLocation: SyncedFileLocation? = null,
	private var _isUsingExistingFiles: Boolean = false,
	private var _isSyncLocalConnectionsOnly: Boolean = false,
	private var _isWakeOnLanEnabled: Boolean = false) {

	val id: Int get() = _id
	val libraryName: String? get() = _libraryName
	val accessCode: String? get() = _accessCode
	val userName: String? get() = _userName
	val password: String? get() = _password
	val isLocalOnly: Boolean get() = _isLocalOnly
	val isRepeating: Boolean get() = _isRepeating
	val nowPlayingId: Int get() = _nowPlayingId
	val nowPlayingProgress: Long get() = _nowPlayingProgress
	val selectedViewType: ViewType? get() = _selectedViewType
	val selectedView: Int get() = _selectedView
	val savedTracksString: String? get() = _savedTracksString
	val customSyncedFilesPath: String? get() = _customSyncedFilesPath
	val syncedFileLocation: SyncedFileLocation? get() = _syncedFileLocation
	val isUsingExistingFiles: Boolean get() = _isUsingExistingFiles
	val isSyncLocalConnectionsOnly: Boolean get() = _isSyncLocalConnectionsOnly
	val isWakeOnLanEnabled: Boolean get() = _isWakeOnLanEnabled

	fun setNowPlayingId(nowPlayingId: Int): Library {
		_nowPlayingId = nowPlayingId
		return this
	}

	fun setLibraryName(libraryName: String?): Library {
		_libraryName = libraryName
		return this
	}

	fun setAccessCode(accessCode: String?): Library {
		_accessCode = accessCode
		return this
	}

	fun setNowPlayingProgress(nowPlayingProgress: Long): Library {
		_nowPlayingProgress = nowPlayingProgress
		return this
	}

	fun setSavedTracksString(savedTracksString: String?): Library {
		_savedTracksString = savedTracksString
		return this
	}

	fun setLocalOnly(isLocalOnly: Boolean): Library {
		_isLocalOnly = isLocalOnly
		return this
	}

	fun setSelectedView(selectedView: Int): Library {
		_selectedView = selectedView
		return this
	}

	fun setRepeating(isRepeating: Boolean): Library {
		_isRepeating = isRepeating
		return this
	}

	fun setCustomSyncedFilesPath(customSyncedFilesPath: String?): Library {
		this._customSyncedFilesPath = customSyncedFilesPath
		return this
	}

	fun setSyncedFileLocation(syncedFileLocation: SyncedFileLocation?): Library {
		_syncedFileLocation = syncedFileLocation
		return this
	}

	fun setIsUsingExistingFiles(isUsingExistingFiles: Boolean): Library {
		_isUsingExistingFiles = isUsingExistingFiles
		return this
	}

	fun setIsSyncLocalConnectionsOnly(isSyncLocalConnections: Boolean): Library {
		_isSyncLocalConnectionsOnly = isSyncLocalConnections
		return this
	}

	fun setIsWakeOnLanEnabled(isWakeOnLanEnabled: Boolean): Library {
		_isWakeOnLanEnabled = isWakeOnLanEnabled
		return this
	}

	fun setSelectedViewType(selectedViewType: ViewType?): Library {
		_selectedViewType = selectedViewType
		return this
	}

	fun setId(id: Int): Library {
		_id = id
		return this
	}

	fun setUserName(userName: String?): Library {
		_userName = userName
		return this
	}

	fun setPassword(password: String?): Library {
		_password = password
		return this
	}

	val libraryId: LibraryId
		get() = LibraryId(id)

	@Keep
	enum class SyncedFileLocation {
		EXTERNAL, INTERNAL, CUSTOM;

		companion object {
			@JvmField
			val ExternalDiskAccessSyncLocations: Set<SyncedFileLocation> = Collections.unmodifiableSet(
				HashSet(
					listOf(EXTERNAL,
						CUSTOM)))
		}
	}

	@Keep
	enum class ViewType {
		StandardServerView, PlaylistView, DownloadView
	}
}
