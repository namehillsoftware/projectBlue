package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

interface GetApplicationSettings {
	fun promiseApplicationSettings(): Promise<ApplicationSettings>
}
