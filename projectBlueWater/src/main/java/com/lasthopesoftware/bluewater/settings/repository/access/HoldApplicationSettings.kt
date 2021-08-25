package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

interface HoldApplicationSettings {
	fun promiseApplicationSettings(): Promise<ApplicationSettings>

	fun promiseUpdatedSettings(applicationSettings: ApplicationSettings): Promise<ApplicationSettings>
}
