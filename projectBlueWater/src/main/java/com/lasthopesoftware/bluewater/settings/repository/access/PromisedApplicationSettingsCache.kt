package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.promises.ResolvedPromiseBox
import com.namehillsoftware.handoff.promises.Promise

object PromisedApplicationSettingsCache : CachePromisedApplicationSettings {
	private val sync = Any()

	@Volatile
	private var promisedApplicationSettings: ResolvedPromiseBox<ApplicationSettings, Promise<ApplicationSettings>>? = null

	override fun getOrSetCachedSettings(factory: () -> Promise<ApplicationSettings>): Promise<ApplicationSettings> =
		promisedApplicationSettings
			?.resolvedPromise
			?: synchronized(sync) {
				promisedApplicationSettings
					?.run {
						resolvedPromise ?: forwardResolution {
							synchronized(sync) {
								if (promisedApplicationSettings === this) promisedApplicationSettings = null
								getOrSetCachedSettings(factory)
							}
						}
					}
					?: factory().also { promisedApplicationSettings = ResolvedPromiseBox(it) }
			}

	override fun setAndGetCachedSettings(updater: (Promise<ApplicationSettings>?) -> Promise<ApplicationSettings>): Promise<ApplicationSettings> =
		synchronized(sync) {
			updater(promisedApplicationSettings?.originalPromise).also { promisedApplicationSettings = ResolvedPromiseBox(it) }
		}
}
