package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.ResolvedPromiseBox
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

object PromisedApplicationSettingsCache : CachePromisedApplicationSettings {
	private val sync = Any()

	@Volatile
	private var promisedApplicationSettings: ResolvedPromiseBox<ApplicationSettings, Promise<ApplicationSettings>>? = null

	override fun getOrSetCachedSettings(factory: () -> Promise<ApplicationSettings>): Promise<ApplicationSettings> =
		synchronized(sync) {
			promisedApplicationSettings
				?.run {
					resolvedPromise ?: originalPromise.eventually(
						{ it.toPromise() },
						{
							synchronized(sync) {
								factory().also { promisedApplicationSettings = ResolvedPromiseBox(it) }
							}
						})
				}
				?: factory().also { promisedApplicationSettings = ResolvedPromiseBox(it) }
		}

	override fun setAndGetCachedSettings(updater: (Promise<ApplicationSettings>?) -> Promise<ApplicationSettings>): Promise<ApplicationSettings> =
		synchronized(sync) {
			updater(promisedApplicationSettings?.originalPromise).also { promisedApplicationSettings = ResolvedPromiseBox(it) }
		}
}
