package com.lasthopesoftware.bluewater.client.browsing.library.repository

import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.repository.IEntityCreator
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import java.util.*

@Keep
class Library : IEntityCreator, IEntityUpdater {
	/**
	 * @return the id
	 */
	var id = -1
		private set

	/**
	 * @return the mLibraryName
	 */
	// Remote connection fields
	var libraryName: String? = null
		private set

	/**
	 * @return the mAccessCode
	 */
	var accessCode: String? = null
		private set
	var userName: String? = null
		private set
	var password: String? = null
		private set

	/**
	 * @return the isLocalOnly
	 */
	var isLocalOnly = false
		private set

	/**
	 * @return the isRepeating
	 */
	var isRepeating = false
		private set

	/**
	 * @return the nowPlayingId
	 */
	var nowPlayingId = -1
		private set

	/**
	 * @return the nowPlayingProgress
	 */
	var nowPlayingProgress: Long = -1
		private set
	var selectedViewType: ViewType? = null
		private set

	/**
	 * @return the selectedView
	 */
	var selectedView = -1
		private set
	var savedTracksString: String? = null
		private set
	var customSyncedFilesPath: String? = null
		private set
	var syncedFileLocation: SyncedFileLocation? = null
		private set
	var isUsingExistingFiles = false
		private set
	var isSyncLocalConnectionsOnly = false
		private set
	var isWakeOnLanEnabled = false
		private set

	/**
	 * @param nowPlayingId the nowPlayingId to set
	 */
	fun setNowPlayingId(nowPlayingId: Int): Library {
		this.nowPlayingId = nowPlayingId
		return this
	}

	/**
	 * @param libraryName the mLibraryName to set
	 */
	fun setLibraryName(libraryName: String?): Library {
		this.libraryName = libraryName
		return this
	}

	/**
	 * @param accessCode the mAccessCode to set
	 */
	fun setAccessCode(accessCode: String?): Library {
		this.accessCode = accessCode
		return this
	}

	/**
	 * @param nowPlayingProgress the nowPlayingProgress to set
	 */
	fun setNowPlayingProgress(nowPlayingProgress: Long): Library {
		this.nowPlayingProgress = nowPlayingProgress
		return this
	}

	fun setSavedTracksString(savedTracksString: String?): Library {
		this.savedTracksString = savedTracksString
		return this
	}

	/**
	 * @param isLocalOnly the isLocalOnly to set
	 */
	fun setLocalOnly(isLocalOnly: Boolean): Library {
		this.isLocalOnly = isLocalOnly
		return this
	}

	/**
	 * @param selectedView the selectedView to set
	 */
	fun setSelectedView(selectedView: Int): Library {
		this.selectedView = selectedView
		return this
	}

	/**
	 * @param isRepeating the isRepeating to set
	 */
	fun setRepeating(isRepeating: Boolean): Library {
		this.isRepeating = isRepeating
		return this
	}

	fun setCustomSyncedFilesPath(customSyncedFilesPath: String?): Library {
		this.customSyncedFilesPath = customSyncedFilesPath
		return this
	}

	fun setSyncedFileLocation(syncedFileLocation: SyncedFileLocation?): Library {
		this.syncedFileLocation = syncedFileLocation
		return this
	}

	fun setIsUsingExistingFiles(isUsingExistingFiles: Boolean): Library {
		this.isUsingExistingFiles = isUsingExistingFiles
		return this
	}

	fun setIsSyncLocalConnectionsOnly(isSyncLocalConnections: Boolean): Library {
		isSyncLocalConnectionsOnly = isSyncLocalConnections
		return this
	}

	fun setIsWakeOnLanEnabled(isWakeOnLanEnabled: Boolean): Library {
		this.isWakeOnLanEnabled = isWakeOnLanEnabled
		return this
	}

	fun setSelectedViewType(selectedViewType: ViewType?): Library {
		this.selectedViewType = selectedViewType
		return this
	}

	fun setId(id: Int): Library {
		this.id = id
		return this
	}

	fun setUserName(userName: String?): Library {
		this.userName = userName
		return this
	}

	fun setPassword(password: String?): Library {
		this.password = password
		return this
	}

	val libraryId: LibraryId
		get() = LibraryId(id)

	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL("CREATE TABLE `LIBRARIES` (" +
			"`accessCode` VARCHAR(30) , " +
			"`userName` VARCHAR , " +
			"`password` VARCHAR , " +
			"`customSyncedFilesPath` VARCHAR , " +
			"`id` INTEGER PRIMARY KEY AUTOINCREMENT , " +
			"`isLocalOnly` SMALLINT , " +
			"`isRepeating` SMALLINT , " +
			"`isSyncLocalConnectionsOnly` SMALLINT , " +
			"`isUsingExistingFiles` SMALLINT , " +
			"`" + isWakeOnLanEnabledColumn + "` SMALLINT , " +
			"`libraryName` VARCHAR(50) , " +
			"`nowPlayingId` INTEGER DEFAULT -1 NOT NULL , " +
			"`nowPlayingProgress` INTEGER DEFAULT -1 NOT NULL , " +
			"`savedTracksString` VARCHAR , " +
			"`selectedView` INTEGER DEFAULT -1 NOT NULL , " +
			"`selectedViewType` VARCHAR , " +
			"`syncedFileLocation` VARCHAR )")
	}

	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 5) {
			db.execSQL("ALTER TABLE `LIBRARIES` add column `customSyncedFilesPath` VARCHAR;")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `syncedFileLocation` VARCHAR DEFAULT 'INTERNAL';")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `isUsingExistingFiles` BOOLEAN DEFAULT 0;")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `isSyncLocalConnectionsOnly` BOOLEAN DEFAULT 0;")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `selectedViewType` VARCHAR;")
		}
		if (oldVersion < 7) {
			db.execSQL("ALTER TABLE `LIBRARIES` add column `userName` VARCHAR;")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `password` VARCHAR;")
			db.rawQuery("SELECT ID, authKey FROM `LIBRARIES` WHERE `authKey` IS NOT NULL AND `authKey` <> ''", arrayOfNulls(0)).use { cursor ->
				if (cursor.moveToFirst() && cursor.count > 0) {
					do {
						val libraryId = cursor.getInt(0)
						val authKey = cursor.getString(1)
						if (authKey == null || authKey.isEmpty()) continue
						val decodedAuthKey = String(Base64.decode(authKey, Base64.DEFAULT))
						val userCredentials = decodedAuthKey.split(":").toTypedArray()
						if (userCredentials.size > 1) {
							db.execSQL(
								"UPDATE `" + tableName + "` " +
									" SET `" + userNameColumn + "` = ?, " +
									" `" + passwordColumn + "` = ? " +
									" WHERE `id` = ?", arrayOf<Any>(
								userCredentials[0],
								userCredentials[1],
								libraryId
							))
							continue
						}
						if (userCredentials.size > 0) {
							db.execSQL(
								"UPDATE `" + tableName + "` " +
									" SET `" + userNameColumn + "` = ? " +
									" WHERE `id` = ?", arrayOf<Any>(
								userCredentials[0],
								libraryId
							))
						}
					} while (cursor.moveToNext())
				}
			}
		}
		if (oldVersion < 8) {
			db.execSQL("ALTER TABLE `LIBRARIES` add column `$isWakeOnLanEnabledColumn` SMALLINT;")
		}
	}

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

	companion object {
		const val tableName = "LIBRARIES"
		const val libraryNameColumn = "libraryName"
		const val accessCodeColumn = "accessCode"
		const val isLocalOnlyColumn = "isLocalOnly"
		const val isRepeatingColumn = "isRepeating"
		const val nowPlayingIdColumn = "nowPlayingId"
		const val nowPlayingProgressColumn = "nowPlayingProgress"
		const val selectedViewTypeColumn = "selectedViewType"
		const val selectedViewColumn = "selectedView"
		const val savedTracksStringColumn = "savedTracksString"
		const val customSyncedFilesPathColumn = "customSyncedFilesPath"
		const val syncedFileLocationColumn = "syncedFileLocation"
		const val isUsingExistingFilesColumn = "isUsingExistingFiles"
		const val isSyncLocalConnectionsOnlyColumn = "isSyncLocalConnectionsOnly"
		const val userNameColumn = "userName"
		const val passwordColumn = "password"
		const val isWakeOnLanEnabledColumn = "isWakeOnLanEnabled"
	}
}
