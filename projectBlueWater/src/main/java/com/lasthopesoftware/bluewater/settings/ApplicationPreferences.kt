package com.lasthopesoftware.bluewater.settings

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType

data class ApplicationPreferences(
	val isSyncOnWifiOnly: Boolean,
	val isSyncOnPowerOnly: Boolean,
	val isVolumeLevelingEnabled: Boolean,
	val playbackEngine: PlaybackEngineType,
	val skipSilence: Boolean)
