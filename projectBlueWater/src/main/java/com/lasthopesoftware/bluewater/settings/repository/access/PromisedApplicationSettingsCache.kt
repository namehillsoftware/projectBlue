package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.updateAndGetIfNull
import com.lasthopesoftware.bluewater.shared.updateAndGetNotNull
import com.lasthopesoftware.promises.ResolvedPromiseBox
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.atomic.AtomicReference

object PromisedApplicationSettingsCache : CachePromisedApplicationSettings {

	@Volatile
	private var promisedApplicationSettings = AtomicReference<ResolvedPromiseBox<ApplicationSettings, Promise<ApplicationSettings>>?>(null)

	override fun getOrSetCachedSettings(factory: () -> Promise<ApplicationSettings>): Promise<ApplicationSettings> =
		promisedApplicationSettings.get()
			?.resolvedPromise
			?: promisedApplicationSettings.get()
				?.run {
					resolvedPromise ?: forwardResolution {
						promisedApplicationSettings.compareAndSet(this, null)
						getOrSetCachedSettings(factory)
					}
				}
				?: promisedApplicationSettings.updateAndGetIfNull { it ?: ResolvedPromiseBox(factory()) }.originalPromise

	override fun setAndGetCachedSettings(updater: (Promise<ApplicationSettings>?) -> Promise<ApplicationSettings>): Promise<ApplicationSettings> =
		promisedApplicationSettings.updateAndGetNotNull { ResolvedPromiseBox(updater(it?.originalPromise)) }.originalPromise
}
