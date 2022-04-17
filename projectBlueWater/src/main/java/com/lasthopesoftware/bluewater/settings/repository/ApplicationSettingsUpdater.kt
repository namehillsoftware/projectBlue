package com.lasthopesoftware.bluewater.settings.repository

import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isUsingCustomCachingColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName

class ApplicationSettingsUpdater(private val applicationSettingsMigrator: ApplicationSettingsMigrator) : IEntityUpdater {
	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 10)
			db.execSQL("ALTER TABLE `$tableName` add column `$isUsingCustomCachingColumn` SMALLINT;")

		if (oldVersion < 9) applicationSettingsMigrator.migrateSettings(db)
	}
}
