package com.lasthopesoftware.bluewater.settings.volumeleveling

import com.namehillsoftware.handoff.promises.Promise

interface ConfigureVolumeLevelling {
	fun promiseIsVolumeLevellingEnabled(): Promise<Boolean>
	fun promiseIsPeakLevelNormalizeEnabled(): Promise<Boolean>
}
