package com.lasthopesoftware.bluewater.settings.repository

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.Keep
import com.google.gson.Gson
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.features.ApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.fetchFirstOrNull
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings.Theme
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.applicationFeatureConfigurationColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.chosenLibraryIdColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.createTableSql
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isLoggingToFile
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isPeakLevelNormalizeEnabledColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnPowerOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnWifiOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isVolumeLevelingEnabledColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.themeColumn
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand

private const val playbackEngineTypeNameColumn = "playbackEngineTypeName"

class ApplicationSettingsUpdater(private val applicationSettingsMigrator: ApplicationSettingsMigrator) : IEntityUpdater {
	private val gson by lazy { Gson() }

	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		fun removeIsUsingCustomCaching(db: SQLiteDatabase) {
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

		fun addIsLoggingToFile() {
			db.execSQL("ALTER table `$tableName` ADD COLUMN `$isLoggingToFile` SMALLINT")
		}

		if (oldVersion < 9) {
			applicationSettingsMigrator.migrateSettings(db)
			return
		}

		if (oldVersion in 10..11) {
			removeIsUsingCustomCaching(db)
		}

		if (oldVersion < 16) {
			addIsLoggingToFile()
		}

		if (oldVersion < 21) {
			db.execSQL("ALTER table `$tableName` ADD COLUMN `$isPeakLevelNormalizeEnabledColumn` SMALLINT DEFAULT 0 NOT NULL")
		}

		if (oldVersion < 22) {
			db.execSQL("ALTER table `$tableName` ADD COLUMN `$themeColumn` VARCHAR")
		}

		if (oldVersion < 23) {
			val tempTableName = tableName + "${tableName}_23"
			val createTempTableSql = createTableSql.replaceFirst("`${tableName}`", "`$tempTableName`")
			db.execSQL(createTempTableSql)

			val oldApplicationSettings = SqLiteCommand(db, "SELECT * FROM $tableName").fetchFirstOrNull<Version22ApplicationSettings>()
			oldApplicationSettings?.toApplicationSettings()?.also { applicationSettings ->
				SqLiteAssistants.insertValue(db, tempTableName, applicationSettings)
			}

			SqLiteCommand(db, "UPDATE $tempTableName SET $applicationFeatureConfigurationColumn = @$applicationFeatureConfigurationColumn")
				.addParameter(
					applicationFeatureConfigurationColumn, gson.toJson(
						ApplicationFeatureConfiguration(
							playbackEngineType = oldApplicationSettings?.playbackEngineTypeName?.let(PlaybackEngineType::valueOf)
								?: PlaybackEngineType.ExoPlayer
						)
					)
				)

			db.execSQL("DROP TABLE `$tableName`")
			db.execSQL("ALTER TABLE `$tempTableName` RENAME TO `$tableName`")
		}
	}

	@Keep
	class Version22ApplicationSettings {
		var isSyncOnWifiOnly: Boolean = false
		var isSyncOnPowerOnly: Boolean = false
		var isVolumeLevelingEnabled: Boolean = false
		var isPeakLevelNormalizeEnabled: Boolean = false
		var isLoggingToFile: Boolean = false
		var playbackEngineTypeName: String? = null
		var chosenLibraryId: Int = -1
		var theme: Theme? = null

		fun toApplicationSettings(): ApplicationSettings = ApplicationSettings(
			isSyncOnWifiOnly = isSyncOnWifiOnly,
			isSyncOnPowerOnly = isSyncOnPowerOnly,
			isVolumeLevelingEnabled = isVolumeLevelingEnabled,
			isPeakLevelNormalizeEnabled = isPeakLevelNormalizeEnabled,
			isLoggingToFile = isLoggingToFile,
			chosenLibraryId = chosenLibraryId,
			theme = theme,
		)
	}
}
