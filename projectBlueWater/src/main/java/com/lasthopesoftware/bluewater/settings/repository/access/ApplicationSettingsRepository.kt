package com.lasthopesoftware.bluewater.settings.repository.access

import android.content.Context
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.lasthopesoftware.bluewater.repository.promiseExecution
import com.lasthopesoftware.bluewater.repository.promiseFirst
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.chosenLibraryColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnPowerOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnWifiOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isVolumeLevelingEnabledColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.playbackEngineColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName
import com.lasthopesoftware.bluewater.shared.promises.promiseUse
import com.namehillsoftware.handoff.promises.Promise

class ApplicationSettingsRepository(private val context: Context): HoldApplicationSettings {

	companion object {
		private val lazyUpdateStatement = lazy {
			UpdateBuilder.fromTable(tableName)
				.addSetter(isSyncOnWifiOnlyColumn)
				.addSetter(isSyncOnPowerOnlyColumn)
				.addSetter(isVolumeLevelingEnabledColumn)
				.addSetter(playbackEngineColumn)
				.addSetter(chosenLibraryColumn)
				.buildQuery()
		}
	}

	override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
		RepositoryAccessHelper(context).promiseUse { r ->
			r.mapSql("SELECT * FROM $tableName").promiseFirst(ApplicationSettings::class.java)
		}

	override fun promiseUpdatedSettings(applicationSettings: ApplicationSettings): Promise<ApplicationSettings> =
		RepositoryAccessHelper(context)
			.promiseUse { r ->
				r.mapSql(lazyUpdateStatement.value)
					.addParameter(isSyncOnWifiOnlyColumn, applicationSettings.isSyncOnWifiOnly)
					.addParameter(isSyncOnPowerOnlyColumn, applicationSettings.isSyncOnPowerOnly)
					.addParameter(isVolumeLevelingEnabledColumn, applicationSettings.isVolumeLevelingEnabled)
					.addParameter(playbackEngineColumn, applicationSettings.playbackEngineType)
					.addParameter(chosenLibraryColumn, applicationSettings.chosenLibraryId)
					.promiseExecution()
			}
			.then { applicationSettings }
}
