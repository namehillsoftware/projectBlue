package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.settings.repository.access.TvApplicationSettingsRepository

class TvApplicationDependencies(
	private val dependencies: ApplicationDependencies
) : ApplicationDependencies by dependencies {
	override val applicationSettings by lazy {
		TvApplicationSettingsRepository(dependencies.applicationSettings)
	}
}
