package com.lasthopesoftware.bluewater.features.access

import android.content.Context
import com.lasthopesoftware.bluewater.features.ApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetchFirstOrNull
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.applicationFeatureConfigurationColumn
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.lasthopesoftware.resources.strings.TranslateJson
import com.lasthopesoftware.resources.strings.parseJson
import com.namehillsoftware.handoff.promises.Promise

class ApplicationFeatureConfigurationRepository(private val context: Context, private val jsonTranslator: TranslateJson) : HoldApplicationFeatureConfiguration {
	override fun promiseFeatureConfiguration(): Promise<ApplicationFeatureConfiguration> = promiseTableMessage {
		RepositoryAccessHelper(context).use { helper ->
			helper.beginNonExclusiveTransaction().use {
				helper.mapSql("SELECT $applicationFeatureConfigurationColumn FROM $tableName").fetchFirstOrNull<String>()
			}
		}
	}.eventually { configuration ->
		ThreadPools.compute.preparePromise {
			configuration?.let(jsonTranslator::parseJson) ?: ApplicationFeatureConfiguration()
		}
	}

	override fun promiseUpdatedFeatureConfiguration(applicationFeatureConfiguration: ApplicationFeatureConfiguration): Promise<ApplicationFeatureConfiguration> = promiseTableMessage {
		RepositoryAccessHelper(context).use { helper ->
			helper.beginTransaction().use {
				helper
					.mapSql("UPDATE $tableName SET $applicationFeatureConfigurationColumn = @$applicationFeatureConfigurationColumn")
					.addParameter(applicationFeatureConfigurationColumn, jsonTranslator.serializeJson(applicationFeatureConfiguration))
					.execute()
				it.setTransactionSuccessful()
			}
		}
	}.eventually {
		promiseFeatureConfiguration()
	}
}
