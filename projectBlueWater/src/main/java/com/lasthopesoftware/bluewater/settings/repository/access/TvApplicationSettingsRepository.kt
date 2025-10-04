package com.lasthopesoftware.bluewater.settings.repository.access

import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class TvApplicationSettingsRepository(private val inner: HoldApplicationSettings) : HoldApplicationSettings by inner {
	override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
		inner
			.promiseApplicationSettings()
			.then {
				val theme = it.theme
				if (theme == null)
					it.theme = ApplicationSettings.Theme.DARK
				it
			}
}
