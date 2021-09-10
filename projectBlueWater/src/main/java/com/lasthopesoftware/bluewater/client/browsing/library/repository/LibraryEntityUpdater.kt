package com.lasthopesoftware.bluewater.client.browsing.library.repository

import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isWakeOnLanEnabledColumn
import com.lasthopesoftware.bluewater.repository.IEntityUpdater

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
								"UPDATE `" + LibraryEntityInformation.tableName + "` " +
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
								"UPDATE `" + LibraryEntityInformation.tableName + "` " +
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
	}
}
