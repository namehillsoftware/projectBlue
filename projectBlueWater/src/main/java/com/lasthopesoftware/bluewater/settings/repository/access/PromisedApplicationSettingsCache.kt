package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

object PromisedApplicationSettingsCache : CachePromisedApplicationSettings {
	@Volatile
	private var promisedApplicationSettings: Promise<ApplicationSettings>? = null

	@Synchronized
	override fun setAndGetCachedSettings(updater: (Promise<ApplicationSettings>?) -> Promise<ApplicationSettings>): Promise<ApplicationSettings> =
		updater(promisedApplicationSettings).also { promisedApplicationSettings = it }
}
