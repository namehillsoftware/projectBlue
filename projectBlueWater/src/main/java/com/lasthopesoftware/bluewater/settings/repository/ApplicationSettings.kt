package com.lasthopesoftware.bluewater.settings.repository

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType

data class ApplicationSettings(
	var isSyncOnWifiOnly: Boolean = false,
	var isSyncOnPowerOnly: Boolean = false,
	var isVolumeLevelingEnabled: Boolean = false,
	var playbackEngineType: PlaybackEngineType,
)
