package com.lasthopesoftware.bluewater.settings.repository

data class ApplicationSettings(
	var isSyncOnWifiOnly: Boolean = false,
	var isSyncOnPowerOnly: Boolean = false,
	var isVolumeLevelingEnabled: Boolean = false,
	var playbackEngineType: String? = null,
	var chosenLibraryId: Int = -1,
)
