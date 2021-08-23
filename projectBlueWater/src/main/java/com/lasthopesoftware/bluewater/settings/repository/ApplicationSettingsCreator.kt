package com.lasthopesoftware.bluewater.settings.repository

import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.repository.IEntityCreator

class ApplicationSettingsCreator(private val applicationSettingsMigrator: ApplicationSettingsMigrator) : IEntityCreator {
	override fun onCreate(db: SQLiteDatabase) {
		applicationSettingsMigrator.migrateSettings(db)
	}
}
