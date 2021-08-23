package com.lasthopesoftware.bluewater.settings.repository

import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.repository.IEntityUpdater

class ApplicationSettingsUpdater(private val applicationSettingsMigrator: ApplicationSettingsMigrator) : IEntityUpdater {
	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 9) applicationSettingsMigrator.migrateSettings(db)
	}
}
