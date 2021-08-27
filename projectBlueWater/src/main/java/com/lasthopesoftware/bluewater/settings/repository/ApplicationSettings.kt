package com.lasthopesoftware.bluewater.settings.repository

data class ApplicationSettings(
	var isSyncOnWifiOnly: Boolean,
	var isSyncOnPowerOnly: Boolean,
	var isVolumeLevelingEnabled: Boolean,
	var playbackEngineType: String,
	var chosenLibraryId: Int,
)
