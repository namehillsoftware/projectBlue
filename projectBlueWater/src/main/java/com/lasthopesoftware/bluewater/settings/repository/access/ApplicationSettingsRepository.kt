package com.lasthopesoftware.bluewater.settings.repository.access

import android.content.Context
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper.Companion.databaseExecutor
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.chosenLibraryIdColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnPowerOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isSyncOnWifiOnlyColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.isVolumeLevelingEnabledColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.playbackEngineColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class ApplicationSettingsRepository(private val context: Context): HoldApplicationSettings {

	companion object {
		private val lazyUpdateStatement by lazy {
			UpdateBuilder.fromTable(tableName)
				.addSetter(isSyncOnWifiOnlyColumn)
				.addSetter(isSyncOnPowerOnlyColumn)
				.addSetter(isVolumeLevelingEnabledColumn)
				.addSetter(playbackEngineColumn)
				.addSetter(chosenLibraryIdColumn)
				.buildQuery()
		}
	}

	override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
		QueuedPromise(MessageWriter {
			RepositoryAccessHelper(context).use { it.mapSql("SELECT * FROM $tableName").fetchFirst() }
		}, databaseExecutor())

	override fun promiseUpdatedSettings(applicationSettings: ApplicationSettings): Promise<ApplicationSettings> =
		QueuedPromise(MessageWriter {
			RepositoryAccessHelper(context).use { helper ->
				helper.beginTransaction().use {
					helper.mapSql(lazyUpdateStatement)
						.addParameter(isSyncOnWifiOnlyColumn, applicationSettings.isSyncOnWifiOnly)
						.addParameter(isSyncOnPowerOnlyColumn, applicationSettings.isSyncOnPowerOnly)
						.addParameter(isVolumeLevelingEnabledColumn, applicationSettings.isVolumeLevelingEnabled)
						.addParameter(playbackEngineColumn, applicationSettings.playbackEngineType)
						.addParameter(chosenLibraryIdColumn, applicationSettings.chosenLibraryId)
						.execute()
				}
			}
		}, databaseExecutor())
			.eventually { promiseApplicationSettings() }
}
