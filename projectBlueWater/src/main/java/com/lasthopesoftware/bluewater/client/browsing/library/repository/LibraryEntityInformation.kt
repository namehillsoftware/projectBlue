package com.lasthopesoftware.bluewater.client.browsing.library.repository

object LibraryEntityInformation {
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
	const val syncedFileLocationColumn = "syncedFileLocation"
	const val isUsingExistingFilesColumn = "isUsingExistingFiles"
	const val isSyncLocalConnectionsOnlyColumn = "isSyncLocalConnectionsOnly"
	const val userNameColumn = "userName"
	const val passwordColumn = "password"
	const val isWakeOnLanEnabledColumn = "isWakeOnLanEnabled"
	const val createTableSql = """CREATE TABLE IF NOT EXISTS `LIBRARIES` (
			`$accessCodeColumn` VARCHAR(30) ,
			`$userNameColumn` VARCHAR ,
			`$passwordColumn` VARCHAR ,
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
			`$syncedFileLocationColumn` VARCHAR )"""
}
