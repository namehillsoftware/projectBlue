package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

interface CachePromisedApplicationSettings {
	fun setAndGetCachedSettings(updater: (Promise<ApplicationSettings>?) -> Promise<ApplicationSettings>): Promise<ApplicationSettings>
}
