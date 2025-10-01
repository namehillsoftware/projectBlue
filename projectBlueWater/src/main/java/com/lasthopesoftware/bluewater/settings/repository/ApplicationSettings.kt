package com.lasthopesoftware.bluewater.settings.repository

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.repository.Entity

@Keep
data class ApplicationSettings(
	var isSyncOnWifiOnly: Boolean = false,
	var isSyncOnPowerOnly: Boolean = false,
	var isVolumeLevelingEnabled: Boolean = false,
	var isPeakLevelNormalizeEnabled: Boolean = false,
	var isLoggingToFile: Boolean = false,
	var playbackEngineTypeName: String? = null,
	var chosenLibraryId: Int = -1,
	var theme: Theme = Theme.SYSTEM,
) : Entity {

	@Keep
	enum class Theme {
		SYSTEM, LIGHT, DARK
	}
}
