package com.lasthopesoftware.bluewater.client.browsing.library.repository

import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.annotation.Keep
import com.google.gson.Gson
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.ServerType
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.connectionSettingsColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.createTableSql
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isRepeatingColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isUsingExistingFilesColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isWakeOnLanEnabledColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.libraryNameColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.nowPlayingIdColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.nowPlayingProgressColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.savedTracksStringColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.serverTypeColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.syncedFileLocationColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.tableName
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.io.fromJson
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand

@OptIn(ExperimentalStdlibApi::class)
object LibraryEntityUpdater : IEntityUpdater {

	private val gson by lazy { Gson() }

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
									" SET `" + LibraryEntityInformation.userNameColumn + "` = ?, " +
									" `" + LibraryEntityInformation.passwordColumn + "` = ? " +
									" WHERE `id` = ?", arrayOf<Any>(
									userCredentials[0],
									userCredentials[1],
									libraryId
								))
							continue
						}
						if (userCredentials.isNotEmpty()) {
							db.execSQL(
								"UPDATE `" + tableName + "` " +
									" SET `" + LibraryEntityInformation.userNameColumn + "` = ? " +
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
			db.execSQL("ALTER TABLE `$tableName` add column `$isWakeOnLanEnabledColumn` SMALLINT;")
		}

		if (oldVersion < 14) {
			val tempTableName = tableName + "Temp"
			db.execSQL("DROP TABLE IF EXISTS `$tempTableName`")
			val createTempTableSql = createTableSql.replaceFirst("`$tableName`", "`$tempTableName`")
			db.execSQL(createTempTableSql)
			val oldLibraries = SqLiteCommand(db, "SELECT * FROM $tableName").fetch<Version13Library>()
			val libraryInsertStatement = libraryInsertStatement(tempTableName)
			for (oldLibrary in oldLibraries) {
				val newLibrary = oldLibrary.toLibrary()

				SqLiteCommand(db, libraryInsertStatement)
					.addParameter("id", newLibrary.id)
					.addParameter(isRepeatingColumn, newLibrary.isRepeating)
					.addParameter(isUsingExistingFilesColumn, newLibrary.isUsingExistingFiles)
					.addParameter(syncedFileLocationColumn, newLibrary.syncedFileLocation)
					.addParameter(libraryNameColumn, newLibrary.libraryName)
					.addParameter(nowPlayingIdColumn, newLibrary.nowPlayingId)
					.addParameter(nowPlayingProgressColumn, newLibrary.nowPlayingProgress)
					.addParameter(savedTracksStringColumn, newLibrary.savedTracksString)
					.addParameter(serverTypeColumn, newLibrary.serverType)
					.addParameter(connectionSettingsColumn, newLibrary.connectionSettings)
					.execute()
			}

			db.execSQL("DROP TABLE `$tableName`")
			db.execSQL("ALTER TABLE `$tempTableName` RENAME TO `$tableName`")

			return
		}

		if (oldVersion < 18) {
			val tempTableName = tableName + "Temp"
			db.execSQL("DROP TABLE IF EXISTS `$tempTableName`")
			val createTempTableSql = createTableSql.replaceFirst("`$tableName`", "`$tempTableName`")
			db.execSQL(createTempTableSql)
			val oldLibraries = SqLiteCommand(db, "SELECT * FROM $tableName").fetch<Version17Library>()
			val libraryInsertStatement = libraryInsertStatement(tempTableName)
			for (oldLibrary in oldLibraries) {
				val newLibrary = oldLibrary.toLibrary()

				SqLiteCommand(db, libraryInsertStatement)
					.addParameter("id", newLibrary.id)
					.addParameter(isRepeatingColumn, newLibrary.isRepeating)
					.addParameter(isUsingExistingFilesColumn, newLibrary.isUsingExistingFiles)
					.addParameter(syncedFileLocationColumn, newLibrary.syncedFileLocation)
					.addParameter(libraryNameColumn, newLibrary.libraryName)
					.addParameter(nowPlayingIdColumn, newLibrary.nowPlayingId)
					.addParameter(nowPlayingProgressColumn, newLibrary.nowPlayingProgress)
					.addParameter(savedTracksStringColumn, newLibrary.savedTracksString)
					.addParameter(serverTypeColumn, newLibrary.serverType)
					.addParameter(connectionSettingsColumn, newLibrary.connectionSettings)
					.execute()
			}

			db.execSQL("DROP TABLE `$tableName`")
			db.execSQL("ALTER TABLE `$tempTableName` RENAME TO `$tableName`")
			return
		}

		if (oldVersion < 19) {
			val tempTableName = tableName + "Temp"
			db.execSQL("DROP TABLE IF EXISTS `$tempTableName`")
			val createTempTableSql = createTableSql.replaceFirst("`$tableName`", "`$tempTableName`")
			db.execSQL(createTempTableSql)
			val oldLibraries = SqLiteCommand(db, "SELECT * FROM $tableName").fetch<Version18Library>()
			val libraryInsertStatement = libraryInsertStatement(tempTableName)
			for (oldLibrary in oldLibraries) {
				val newLibrary = oldLibrary.toLibrary()

				SqLiteCommand(db, libraryInsertStatement)
					.addParameter("id", newLibrary.id)
					.addParameter(isRepeatingColumn, newLibrary.isRepeating)
					.addParameter(isUsingExistingFilesColumn, newLibrary.isUsingExistingFiles)
					.addParameter(syncedFileLocationColumn, newLibrary.syncedFileLocation)
					.addParameter(libraryNameColumn, newLibrary.libraryName)
					.addParameter(nowPlayingIdColumn, newLibrary.nowPlayingId)
					.addParameter(nowPlayingProgressColumn, newLibrary.nowPlayingProgress)
					.addParameter(savedTracksStringColumn, newLibrary.savedTracksString)
					.addParameter(serverTypeColumn, newLibrary.serverType)
					.addParameter(connectionSettingsColumn, newLibrary.connectionSettings)
					.execute()
			}

			db.execSQL("DROP TABLE `$tableName`")
			db.execSQL("ALTER TABLE `$tempTableName` RENAME TO `$tableName`")
		}
	}

	@Keep
	class Version13Library {
		var id: Int = 0
		var libraryName: String? = null
		var accessCode: String? = null
		var userName: String? = null
		var password: String? = null
		var isLocalOnly: Boolean = false
		var isRepeating: Boolean = false
		var nowPlayingId: Int = 0
		var nowPlayingProgress: Long = 0L
		var savedTracksString: String? = null
		var syncedFileLocation: SyncedFileLocation? = null
		var isUsingExistingFiles: Boolean = false
		var isSyncLocalConnectionsOnly: Boolean = false
		var isWakeOnLanEnabled: Boolean = false

		fun toLibrary(): Library {
			return Library(
				id = id,
				libraryName = libraryName,
				isRepeating = isRepeating,
                isUsingExistingFiles = isUsingExistingFiles,
				nowPlayingId = nowPlayingId,
				nowPlayingProgress = nowPlayingProgress,
				savedTracksString = savedTracksString,
				serverType = ServerType.MediaCenter.name,
				syncedFileLocation = when (syncedFileLocation) {
					SyncedFileLocation.CUSTOM, SyncedFileLocation.EXTERNAL -> com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation.EXTERNAL
					SyncedFileLocation.INTERNAL -> com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation.INTERNAL
					null -> null
				},
				connectionSettings = gson.toJson(
					StoredMediaCenterConnectionSettings(
						accessCode = accessCode ?: "",
						userName = userName,
						password = password,
						isLocalOnly = isLocalOnly,
						isWakeOnLanEnabled = isWakeOnLanEnabled,
						isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly,
					)
				)
			)
		}

		@Keep
		enum class SyncedFileLocation {
			EXTERNAL, INTERNAL, CUSTOM;
		}
	}

	@Keep
	class Version17Library {
		var id: Int = -1
		var libraryName: String? = null
		var accessCode: String? = null
		var userName: String? = null
		var password: String? = null
		var isLocalOnly: Boolean = false
		var isRepeating: Boolean = false
		var nowPlayingId: Int = -1
		var nowPlayingProgress: Long = -1
		var savedTracksString: String? = null
		var syncedFileLocation: SyncedFileLocation? = null
		var isUsingExistingFiles: Boolean = false
		var isSyncLocalConnectionsOnly: Boolean = false
		var isWakeOnLanEnabled: Boolean = false
		var sslCertificateFingerprint: ByteArray = emptyByteArray
		var macAddress: String? = null

		fun toLibrary(): Library {
			return Library(
				id = id,
				libraryName = libraryName,
				isRepeating = isRepeating,
				nowPlayingId = nowPlayingId,
				nowPlayingProgress = nowPlayingProgress,
				savedTracksString = savedTracksString,
				isUsingExistingFiles = isUsingExistingFiles,
				serverType = ServerType.MediaCenter.name,
				syncedFileLocation = syncedFileLocation,
				connectionSettings = gson.toJson(
					StoredMediaCenterConnectionSettings(
						accessCode = accessCode ?: "",
						userName = userName,
						password = password,
						isLocalOnly = isLocalOnly,
						isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly,
						isWakeOnLanEnabled = isWakeOnLanEnabled,
						sslCertificateFingerprint = sslCertificateFingerprint.toHexString(),
						macAddress = macAddress,
					)
				)
			)
		}
	}

	@Keep
	class Version18Library {
		var id: Int = -1
		var libraryName: String? = null
		var isRepeating: Boolean = false
		var nowPlayingId: Int = -1
		var nowPlayingProgress: Long = -1
		var savedTracksString: String? = null
		var isUsingExistingFiles: Boolean = false
		var serverType: ServerType? = null
		var connectionSettings: String? = null

		fun toLibrary(): Library {
			val connectionSettings = connectionSettings?.let { gson.fromJson<Version18ConnectionSettings>(it) }

			return Library(
				id = id,
				libraryName = libraryName,
				isRepeating = isRepeating,
				nowPlayingId = nowPlayingId,
				nowPlayingProgress = nowPlayingProgress,
				savedTracksString = savedTracksString,
				isUsingExistingFiles = isUsingExistingFiles,
				serverType = serverType?.name,
				syncedFileLocation = connectionSettings?.syncedFileLocation,
				connectionSettings = gson.toJson(
					StoredMediaCenterConnectionSettings(
						accessCode = connectionSettings?.accessCode,
						userName = connectionSettings?.userName,
						password = connectionSettings?.password,
						isLocalOnly = connectionSettings?.isLocalOnly ?: false,
						isSyncLocalConnectionsOnly = connectionSettings?.isSyncLocalConnectionsOnly ?: false,
						isWakeOnLanEnabled = connectionSettings?.isWakeOnLanEnabled ?: false,
						sslCertificateFingerprint = connectionSettings?.sslCertificateFingerprint,
						macAddress = connectionSettings?.macAddress,
					)
				)
			)
		}

		@Keep
		private class Version18ConnectionSettings(
			val accessCode: String? = null,
			val userName: String? = null,
			val password: String? = null,
			val isLocalOnly: Boolean = false,
			val isSyncLocalConnectionsOnly: Boolean = false,
			val syncedFileLocation: SyncedFileLocation? = null,
			val isWakeOnLanEnabled: Boolean = false,
			val sslCertificateFingerprint: String? = null,
			val macAddress: String? = null,
		)
	}

	private fun libraryInsertStatement(tableName: String) = SqLiteAssistants.InsertBuilder
		.fromTable(tableName)
		.addColumn("id")
		.addColumn(isRepeatingColumn)
		.addColumn(isUsingExistingFilesColumn)
		.addColumn(syncedFileLocationColumn)
		.addColumn(libraryNameColumn)
		.addColumn(nowPlayingIdColumn)
		.addColumn(nowPlayingProgressColumn)
		.addColumn(savedTracksStringColumn)
		.addColumn(serverTypeColumn)
		.addColumn(connectionSettingsColumn)
		.buildQuery()
}
