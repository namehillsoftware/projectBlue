package com.lasthopesoftware.bluewater.client.browsing.library.repository

object LibraryEntityInformation {
	const val tableName = "LIBRARIES"
	const val libraryNameColumn = "libraryName"
	const val isRepeatingColumn = "isRepeating"
	const val nowPlayingIdColumn = "nowPlayingId"
	const val nowPlayingProgressColumn = "nowPlayingProgress"
	const val savedTracksStringColumn = "savedTracksString"
	const val syncedFileLocationColumn = "syncedFileLocation"
	const val isUsingExistingFilesColumn = "isUsingExistingFiles"
	const val userNameColumn = "userName"
	const val passwordColumn = "password"
	const val isWakeOnLanEnabledColumn = "isWakeOnLanEnabled"
	const val connectionSettingsColumn = "connectionSettings"
	const val serverTypeColumn = "serverType"
	const val createTableSql = """CREATE TABLE IF NOT EXISTS `LIBRARIES` (
			`id` INTEGER PRIMARY KEY AUTOINCREMENT ,
			`$isRepeatingColumn` SMALLINT ,
			`$isUsingExistingFilesColumn` SMALLINT ,
			`$syncedFileLocationColumn` VARCHAR ,
			`$libraryNameColumn` VARCHAR(50) ,
			`$nowPlayingIdColumn` INTEGER DEFAULT -1 NOT NULL ,
			`$nowPlayingProgressColumn` INTEGER DEFAULT -1 NOT NULL ,
			`$savedTracksStringColumn` VARCHAR ,
			`$serverTypeColumn` VARCHAR ,
			`$connectionSettingsColumn` VARCHAR
			)"""
}
