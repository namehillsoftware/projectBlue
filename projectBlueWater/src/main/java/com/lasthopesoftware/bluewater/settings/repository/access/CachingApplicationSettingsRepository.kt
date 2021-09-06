package com.lasthopesoftware.bluewater.settings.repository.access

import android.content.Context
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class CachingApplicationSettingsRepository
(
	private val inner: HoldApplicationSettings,
	private val applicationSettingsCache: CachePromisedApplicationSettings = PromisedApplicationSettingsCache
) : HoldApplicationSettings {

	companion object {
		fun Context.getApplicationSettingsRepository(): CachingApplicationSettingsRepository =
			CachingApplicationSettingsRepository(ApplicationSettingsRepository(this), PromisedApplicationSettingsCache)
	}

	override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
		applicationSettingsCache.setAndGetCachedSettings { s -> s ?: inner.promiseApplicationSettings() }

	override fun promiseUpdatedSettings(applicationSettings: ApplicationSettings): Promise<ApplicationSettings> =
		applicationSettingsCache.setAndGetCachedSettings { promisedApplicationSettings ->
			promisedApplicationSettings
				?.eventually { inner.promiseUpdatedSettings(applicationSettings) }
				?: inner.promiseUpdatedSettings(applicationSettings)
		}
}
