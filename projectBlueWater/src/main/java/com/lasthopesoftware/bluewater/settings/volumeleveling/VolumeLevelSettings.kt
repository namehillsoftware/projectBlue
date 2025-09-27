package com.lasthopesoftware.bluewater.settings.volumeleveling

import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class VolumeLevelSettings(private val applicationSettings: HoldApplicationSettings) : ConfigureVolumeLevelling {
	override fun promiseIsVolumeLevellingEnabled(): Promise<Boolean> = applicationSettings.promiseApplicationSettings()
		.then { s -> s.isVolumeLevelingEnabled }

	override fun promiseIsPeakLevelNormalizeEnabled(): Promise<Boolean> = applicationSettings.promiseApplicationSettings()
		.then { settings -> settings.isPeakLevelNormalizeEnabled }
}
