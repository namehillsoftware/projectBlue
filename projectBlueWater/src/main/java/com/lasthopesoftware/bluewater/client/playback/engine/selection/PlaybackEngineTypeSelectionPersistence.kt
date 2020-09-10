package com.lasthopesoftware.bluewater.client.playback.engine.selection

import android.content.SharedPreferences
import com.lasthopesoftware.bluewater.ApplicationConstants
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster

class PlaybackEngineTypeSelectionPersistence(private val sharedPreferences: SharedPreferences, private val playbackEngineTypeChangedBroadcaster: PlaybackEngineTypeChangedBroadcaster) : SelectPlaybackEngineType {
	override fun selectPlaybackEngine(playbackEngineType: PlaybackEngineType) {
		sharedPreferences.edit()
			.putString(ApplicationConstants.PreferenceConstants.playbackEngine, playbackEngineType.name)
			.apply()
		playbackEngineTypeChangedBroadcaster.broadcastPlaybackEngineTypeChanged(playbackEngineType)
	}
}
