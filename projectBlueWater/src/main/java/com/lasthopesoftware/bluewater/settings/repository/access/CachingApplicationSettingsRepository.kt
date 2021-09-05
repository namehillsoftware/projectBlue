package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class CachingApplicationSettingsRepository(private val inner: HoldApplicationSettings) : HoldApplicationSettings {

	private val syncObj = Any()

	@Volatile
	private var applicationSettings: ApplicationSettings? = null

	@Volatile
	private var promisedApplicationSettings:  Promise<ApplicationSettings>? = null

	override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
		synchronized(syncObj) {
			applicationSettings?.toPromise() ?: promisedApplicationSettings ?: inner.promiseApplicationSettings().then { s ->
				applicationSettings = s
				s
			}.also { promisedApplicationSettings = it }
		}

	override fun promiseUpdatedSettings(applicationSettings: ApplicationSettings): Promise<ApplicationSettings> =
		synchronized(syncObj) {
			this.applicationSettings = applicationSettings
			inner.promiseUpdatedSettings(applicationSettings)
			applicationSettings.toPromise().also { promisedApplicationSettings = it }
		}
}
