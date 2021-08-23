package com.lasthopesoftware.bluewater.settings.repository

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType

data class ApplicationSettings(
	var isSyncOnWifiOnly: Boolean,
	var isSyncOnPowerOnly: Boolean,
	var isVolumeLevelingEnabled: Boolean,
	var playbackEngineType: PlaybackEngineType,
	var chosenLibraryId: Int,
)
