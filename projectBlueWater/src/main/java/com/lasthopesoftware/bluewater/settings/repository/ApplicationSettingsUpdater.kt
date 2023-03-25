package com.lasthopesoftware.bluewater.settings.repository

import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.chosenLibraryIdColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnPowerOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnWifiOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isVolumeLevelingEnabledColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.playbackEngineTypeNameColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName

class ApplicationSettingsUpdater(private val applicationSettingsMigrator: ApplicationSettingsMigrator) : IEntityUpdater {
	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 9) {
			applicationSettingsMigrator.migrateSettings(db)
			return
		}

		if (oldVersion in 10..11) {
			removeIsUsingCustomCaching(db)
		}
	}

	private fun removeIsUsingCustomCaching(db: SQLiteDatabase) {
		val tempTableName = "${tableName}_12"

		db.execSQL("DROP TABLE IF EXISTS `$tempTableName`")

		db.execSQL(
			"""CREATE TABLE `$tempTableName` (
				`id` INTEGER DEFAULT 1 UNIQUE ,
				`$isSyncOnWifiOnlyColumn` SMALLINT ,
				`$isSyncOnPowerOnlyColumn` SMALLINT ,
				`$isVolumeLevelingEnabledColumn` SMALLINT ,
				`$playbackEngineTypeNameColumn` VARCHAR ,
				`$chosenLibraryIdColumn` INTEGER DEFAULT -1 NOT NULL )"""
		)

		db.execSQL(
			"""
INSERT INTO `$tempTableName`
SELECT
	`id`,
	`$isSyncOnWifiOnlyColumn`,
	`$isSyncOnPowerOnlyColumn`,
	`$isVolumeLevelingEnabledColumn`,
	`$playbackEngineTypeNameColumn`,
	`$chosenLibraryIdColumn`
FROM `$tableName`
""")

		db.execSQL("DROP TABLE `$tableName`")
		db.execSQL("ALTER TABLE `$tempTableName` RENAME TO `$tableName`")
	}
}
