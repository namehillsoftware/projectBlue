package com.lasthopesoftware.bluewater.settings.repository.access

import android.content.Context
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.chosenLibraryIdColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isLoggingToFile
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnPowerOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnWifiOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isVolumeLevelingEnabledColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.playbackEngineTypeNameColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise

class ApplicationSettingsRepository(private val context: Context): HoldApplicationSettings {

	companion object {
		private val updateStatement by lazy {
			UpdateBuilder.fromTable(tableName)
				.addSetter(isSyncOnWifiOnlyColumn)
				.addSetter(isSyncOnPowerOnlyColumn)
				.addSetter(isVolumeLevelingEnabledColumn)
				.addSetter(isLoggingToFile)
				.addSetter(playbackEngineTypeNameColumn)
				.addSetter(chosenLibraryIdColumn)
				.buildQuery()
		}
	}

	override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
		promiseTableMessage<ApplicationSettings, ApplicationSettings> {
			RepositoryAccessHelper(context).use { helper ->
				helper.beginNonExclusiveTransaction().use {
					helper.mapSql("SELECT * FROM $tableName").fetchFirst()
				}
			}
		}

	override fun promiseUpdatedSettings(applicationSettings: ApplicationSettings): Promise<ApplicationSettings> =
		promiseTableMessage<Unit, ApplicationSettings> {
			RepositoryAccessHelper(context).use { helper ->
				helper.beginTransaction().use {
					helper.mapSql(updateStatement)
						.addParameter(isSyncOnWifiOnlyColumn, applicationSettings.isSyncOnWifiOnly)
						.addParameter(isSyncOnPowerOnlyColumn, applicationSettings.isSyncOnPowerOnly)
						.addParameter(isVolumeLevelingEnabledColumn, applicationSettings.isVolumeLevelingEnabled)
						.addParameter(isLoggingToFile, applicationSettings.isLoggingToFile)
						.addParameter(playbackEngineTypeNameColumn, applicationSettings.playbackEngineTypeName)
						.addParameter(chosenLibraryIdColumn, applicationSettings.chosenLibraryId)
						.execute()
					it.setTransactionSuccessful()
				}
			}
		}.eventually { promiseApplicationSettings() }
}
