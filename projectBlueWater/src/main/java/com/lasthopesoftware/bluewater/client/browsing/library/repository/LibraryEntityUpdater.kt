package com.lasthopesoftware.bluewater.client.browsing.library.repository

import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.createTableSql
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isWakeOnLanEnabledColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.tableName
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.fetch
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand

object LibraryEntityUpdater : IEntityUpdater {

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
			db.execSQL("ALTER TABLE `LIBRARIES` add column `$isWakeOnLanEnabledColumn` SMALLINT;")
		}

		if (oldVersion < 14) {
			val tempTableName = tableName + "Temp"
			db.execSQL("DROP TABLE IF EXISTS `$tempTableName`")
			val createTempTableSql = createTableSql.replaceFirst("`$tableName`", "`$tempTableName`")
			db.execSQL(createTempTableSql)
			val oldLibraries = SqLiteCommand(db, "SELECT * FROM $tableName").fetch<Version13Library>()
			for (oldLibrary in oldLibraries) {
				val newLibrary = oldLibrary.toLibrary()

				SqLiteAssistants.insertValue(db, tempTableName, newLibrary)
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
		var selectedViewType: Library.ViewType? = null
		var selectedView: Int = 0
		var savedTracksString: String? = null
		var syncedFileLocation: Version13SyncedFileLocation? = null
		var isUsingExistingFiles: Boolean = false
		var isSyncLocalConnectionsOnly: Boolean = false
		var isWakeOnLanEnabled: Boolean = false

		fun toLibrary(): Library {
			return Library(
				id = id,
				accessCode = accessCode,
				isLocalOnly = isLocalOnly,
				isRepeating = isRepeating,
				isSyncLocalConnectionsOnly = isSyncLocalConnectionsOnly,
				libraryName = libraryName,
				isUsingExistingFiles = isUsingExistingFiles,
				isWakeOnLanEnabled = isWakeOnLanEnabled,
				nowPlayingId = nowPlayingId,
				password = password,
				nowPlayingProgress = nowPlayingProgress,
				savedTracksString = savedTracksString,
				selectedView = selectedView,
				selectedViewType = selectedViewType,
				userName = userName,
				syncedFileLocation = when (syncedFileLocation) {
					Version13SyncedFileLocation.CUSTOM, Version13SyncedFileLocation.EXTERNAL -> Library.SyncedFileLocation.EXTERNAL
					Version13SyncedFileLocation.INTERNAL -> Library.SyncedFileLocation.INTERNAL
					null -> null
				}
			)
		}
	}

	@Keep
	enum class Version13SyncedFileLocation {
		EXTERNAL, INTERNAL, CUSTOM;
	}
}
