package com.lasthopesoftware.bluewater.tutorials

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.preference.PreferenceManager
import com.lasthopesoftware.bluewater.repository.IEntityCreator
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.InsertBuilder
import com.lasthopesoftware.bluewater.tutorials.DisplayedTutorialEntityInformation.tableName
import com.lasthopesoftware.bluewater.tutorials.DisplayedTutorialEntityInformation.tutorialKeyColumn
import com.namehillsoftware.artful.Artful

class TutorialMigrator(private val context: Context) : IEntityCreator, IEntityUpdater {
	companion object {
		private const val checkIfTableExists = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$tableName';"

		private object OldConstants {
			const val isListTutorialShownPreference = "isListTutorialShownPreference"
			const val isApplicationSettingsTutorialShownPreference = "isApplicationSettingsTutorialShownPreference"
		}
	}

	override fun onCreate(db: SQLiteDatabase) {
		migrateSettings(db)
	}

	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 9) migrateSettings(db)
	}

	private fun migrateSettings(db: SQLiteDatabase) {
		val artful = Artful(db, checkIfTableExists)
		val count = artful.execute()

		if (count > 0) return

		db.execSQL("""CREATE TABLE `$tableName` (
			`id` INTEGER PRIMARY KEY AUTOINCREMENT ,
			`$tutorialKeyColumn` VARCHAR UNIQUE
			)""")

		val insertQuery = InsertBuilder.fromTable(tableName)
			.addColumn(tutorialKeyColumn)
			.build()

		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		val insertArtSql = Artful(db, insertQuery)

		if (sharedPreferences.getBoolean(OldConstants.isListTutorialShownPreference, false)) {
			insertArtSql
				.addParameter(tutorialKeyColumn, TutorialManager.KnownTutorials.longPressListTutorial)
				.execute()
		}

		if (sharedPreferences.getBoolean(OldConstants.isApplicationSettingsTutorialShownPreference, false)) {
			insertArtSql
				.addParameter(tutorialKeyColumn, TutorialManager.KnownTutorials.adjustNotificationInApplicationSettingsTutorial)
				.execute()
		}
	}
}
