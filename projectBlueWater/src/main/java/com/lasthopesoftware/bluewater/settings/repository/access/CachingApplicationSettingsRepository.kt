package com.lasthopesoftware.bluewater.settings.repository.access

import android.content.Context
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class CachingApplicationSettingsRepository(private val inner: HoldApplicationSettings) : HoldApplicationSettings {

	companion object {
		private val syncObj = Any()

		private var applicationSettings: CachingApplicationSettingsRepository? = null

		fun Context.getApplicationSettings(): CachingApplicationSettingsRepository =
			applicationSettings ?: synchronized(syncObj) {
				applicationSettings ?: CachingApplicationSettingsRepository(
					ApplicationSettingsRepository(this)
				).also { applicationSettings = it }
			}
	}

	private val syncObj = Any()

	@Volatile
	private var promisedApplicationSettings: Promise<ApplicationSettings>? = null

	override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
		synchronized(syncObj) {
			promisedApplicationSettings
				?: inner.promiseApplicationSettings().updatePromisedInstance()
		}

	override fun promiseUpdatedSettings(applicationSettings: ApplicationSettings): Promise<ApplicationSettings> =
		synchronized(syncObj) {
			promisedApplicationSettings
				?.eventually { inner.promiseUpdatedSettings(applicationSettings) }
				?.updatePromisedInstance()
				?: inner.promiseUpdatedSettings(applicationSettings).updatePromisedInstance()
		}

	private fun Promise<ApplicationSettings>.updatePromisedInstance(): Promise<ApplicationSettings> {
		promisedApplicationSettings = this
		return this
	}
}
