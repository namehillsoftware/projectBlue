package com.lasthopesoftware.bluewater.client.playback.engine.selection

import android.content.SharedPreferences
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.LookupDefaultPlaybackEngine
import com.lasthopesoftware.bluewater.settings.repository.ApplicationConstants
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedPlaybackEngineTypeAccess(private val sharedPreferences: SharedPreferences, private val defaultPlaybackEngineLookup: LookupDefaultPlaybackEngine) : LookupSelectedPlaybackEngineType {
	override fun promiseSelectedPlaybackEngineType(): Promise<PlaybackEngineType> {
		val playbackEngineTypeString = sharedPreferences
			.getString(
				ApplicationConstants.PreferenceConstants.playbackEngine,
				null)

		return if (playbackEngineTypeString != null && PlaybackEngineType.values().any { e -> playbackEngineTypeString == e.name }) PlaybackEngineType.valueOf(playbackEngineTypeString).toPromise()
		else defaultPlaybackEngineLookup.promiseDefaultEngineType()
			.then { t ->
				sharedPreferences
					.edit()
					.putString(
						ApplicationConstants.PreferenceConstants.playbackEngine,
						t.name)
					.apply()

				PlaybackEngineType.valueOf(sharedPreferences
					.getString(
						ApplicationConstants.PreferenceConstants.playbackEngine,
						PlaybackEngineType.ExoPlayer.name)!!)
			}
	}
}
