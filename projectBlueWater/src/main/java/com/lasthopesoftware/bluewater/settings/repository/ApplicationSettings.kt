package com.lasthopesoftware.bluewater.settings.repository

import androidx.annotation.Keep

@Keep
data class ApplicationSettings(
	var isSyncOnWifiOnly: Boolean = false,
	var isSyncOnPowerOnly: Boolean = false,
	var isVolumeLevelingEnabled: Boolean = false,
	var playbackEngineTypeName: String? = null,
	var chosenLibraryId: Int = -1,
)
