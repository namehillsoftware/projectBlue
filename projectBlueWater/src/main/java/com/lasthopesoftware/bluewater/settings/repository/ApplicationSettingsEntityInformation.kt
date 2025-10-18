package com.lasthopesoftware.bluewater.settings.repository

object ApplicationSettingsEntityInformation {
	const val isSyncOnWifiOnlyColumn = "isSyncOnWifiOnly"
	const val isSyncOnPowerOnlyColumn = "isSyncOnPowerOnly"
	const val isVolumeLevelingEnabledColumn = "isVolumeLevelingEnabled"
	const val isPeakLevelNormalizeEnabledColumn = "isPeakLevelNormalizeEnabled"
	const val isLoggingToFile = "isLoggingToFile"
	const val chosenLibraryIdColumn = "chosenLibraryId"
	const val themeColumn = "theme"
	const val applicationFeatureConfigurationColumn = "applicationFeatureConfiguration"
	const val tableName = "ApplicationSettings"
	const val createTableSql = """CREATE TABLE `$tableName` (
			`id` INTEGER DEFAULT 1 UNIQUE ,
			`$isSyncOnWifiOnlyColumn` SMALLINT ,
			`$isSyncOnPowerOnlyColumn` SMALLINT ,
			`$isVolumeLevelingEnabledColumn` SMALLINT ,
			`$isPeakLevelNormalizeEnabledColumn` SMALLINT DEFAULT 0 NOT NULL,
			`$isLoggingToFile` SMALLINT DEFAULT 0 NOT NULL,
			`$applicationFeatureConfigurationColumn` VARCHAR ,
			`$themeColumn` VARCHAR ,
			`$chosenLibraryIdColumn` INTEGER DEFAULT -1 NOT NULL )"""
}
