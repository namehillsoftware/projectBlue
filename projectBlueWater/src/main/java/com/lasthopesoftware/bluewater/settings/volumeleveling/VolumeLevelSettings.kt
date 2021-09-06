package com.lasthopesoftware.bluewater.settings.volumeleveling

import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class VolumeLevelSettings(private val applicationSettings: HoldApplicationSettings) : IVolumeLevelSettings {
    override val isVolumeLevellingEnabled: Promise<Boolean>
		get() = applicationSettings.promiseApplicationSettings()
			.then { s -> s.isVolumeLevelingEnabled }
}
