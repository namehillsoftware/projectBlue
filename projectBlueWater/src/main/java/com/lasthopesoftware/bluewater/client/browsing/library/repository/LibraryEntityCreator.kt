package com.lasthopesoftware.bluewater.client.browsing.library.repository

import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isWakeOnLanEnabledColumn
import com.lasthopesoftware.bluewater.repository.IEntityCreator

object LibraryEntityCreator : IEntityCreator {
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
}
