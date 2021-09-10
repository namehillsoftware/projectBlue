package com.lasthopesoftware.bluewater.settings.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.preference.PreferenceManager
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.repository.InsertBuilder
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.chosenLibraryIdColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnPowerOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnWifiOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isVolumeLevelingEnabledColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.playbackEngineTypeNameColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName
import com.namehillsoftware.artful.Artful

class ApplicationSettingsMigrator(private val context: Context) {
	companion object {
		private const val checkIfTableExists = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$tableName';"

		private object OldConstants {
			const val isSyncOnWifiOnlyKey = "isSyncOnWifiOnly"
			const val isSyncOnPowerOnlyKey = "isSyncOnPowerOnly"
			const val isVolumeLevelingEnabled = "isVolumeLevelingEnabled"
			const val playbackEngine = "playbackEngine"
			const val chosenLibraryKey = "chosen_library"
		}
	}

	fun migrateSettings(db: SQLiteDatabase) {
		val artful = Artful(db, checkIfTableExists)
		val count = artful.execute()

		if (count > 0) return

		db.execSQL("""CREATE TABLE `$tableName` (
			`id` INTEGER DEFAULT 1 UNIQUE ,
			`$isSyncOnWifiOnlyColumn` SMALLINT ,
			`$isSyncOnPowerOnlyColumn` SMALLINT ,
			`$isVolumeLevelingEnabledColumn` SMALLINT ,
			`$playbackEngineTypeNameColumn` VARCHAR ,
			`$chosenLibraryIdColumn` INTEGER DEFAULT -1 NOT NULL )""")

		val insertQuery = InsertBuilder.fromTable(tableName)
			.addColumn(isSyncOnWifiOnlyColumn)
			.addColumn(isSyncOnPowerOnlyColumn)
			.addColumn(isVolumeLevelingEnabledColumn)
			.addColumn(playbackEngineTypeNameColumn)
			.addColumn(chosenLibraryIdColumn)
			.build()

		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		val insertArtSql = Artful(db, insertQuery)
		insertArtSql
			.addParameter(isSyncOnWifiOnlyColumn, sharedPreferences.getBoolean(OldConstants.isSyncOnWifiOnlyKey, false))
			.addParameter(isSyncOnPowerOnlyColumn, sharedPreferences.getBoolean(OldConstants.isSyncOnPowerOnlyKey, false))
			.addParameter(isVolumeLevelingEnabledColumn, sharedPreferences.getBoolean(OldConstants.isVolumeLevelingEnabled, false))
			.addParameter(playbackEngineTypeNameColumn, sharedPreferences.getString(OldConstants.playbackEngine, PlaybackEngineType.ExoPlayer.name))
			.addParameter(chosenLibraryIdColumn, sharedPreferences.getInt(OldConstants.chosenLibraryKey, -1))
			.execute()
	}
}
