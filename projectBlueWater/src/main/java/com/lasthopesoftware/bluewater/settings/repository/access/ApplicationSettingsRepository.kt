package com.lasthopesoftware.bluewater.settings.repository.access

import android.content.Context
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.promiseFirst
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsEntityInformation.tableName
import com.lasthopesoftware.bluewater.shared.promises.promiseUse
import com.namehillsoftware.handoff.promises.Promise

class ApplicationSettingsRepository(private val context: Context): GetApplicationSettings {
	override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
		RepositoryAccessHelper(context).promiseUse { r ->
			r.mapSql("SELECT * FROM $tableName").promiseFirst(ApplicationSettings::class.java)
		}
}
