package com.lasthopesoftware.bluewater.client.browsing.library.repository

import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.accessCodeColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.customSyncedFilesPathColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isLocalOnlyColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isRepeatingColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isSyncLocalConnectionsOnlyColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isUsingExistingFilesColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.isWakeOnLanEnabledColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.libraryNameColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.nowPlayingIdColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.nowPlayingProgressColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.passwordColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.savedTracksStringColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.selectedViewColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.selectedViewTypeColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.syncedFileLocationColumn
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.userNameColumn
import com.lasthopesoftware.bluewater.repository.IEntityCreator

object LibraryEntityCreator : IEntityCreator {
	override fun onCreate(db: SQLiteDatabase) =
		db.execSQL("""CREATE TABLE `LIBRARIES` (
			`$accessCodeColumn` VARCHAR(30) ,
			`$userNameColumn` VARCHAR ,
			`$passwordColumn` VARCHAR ,
			`$customSyncedFilesPathColumn` VARCHAR ,
			`id` INTEGER PRIMARY KEY AUTOINCREMENT ,
			`$isLocalOnlyColumn` SMALLINT ,
			`$isRepeatingColumn` SMALLINT ,
			`$isSyncLocalConnectionsOnlyColumn` SMALLINT ,
			`$isUsingExistingFilesColumn` SMALLINT ,
			`$isWakeOnLanEnabledColumn` SMALLINT ,
			`$libraryNameColumn` VARCHAR(50) ,
			`$nowPlayingIdColumn` INTEGER DEFAULT -1 NOT NULL ,
			`$nowPlayingProgressColumn` INTEGER DEFAULT -1 NOT NULL ,
			`$savedTracksStringColumn` VARCHAR ,
			`$selectedViewColumn` INTEGER DEFAULT -1 NOT NULL ,
			`$selectedViewTypeColumn` VARCHAR ,
			`$syncedFileLocationColumn` VARCHAR )""")
}
