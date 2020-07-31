package com.lasthopesoftware.bluewater.client.playback.engine.selection

import android.content.Context
import android.preference.PreferenceManager
import com.lasthopesoftware.bluewater.ApplicationConstants
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.LookupDefaultPlaybackEngine
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedPlaybackEngineTypeAccess(private val context: Context, private val defaultPlaybackEngineLookup: LookupDefaultPlaybackEngine) : LookupSelectedPlaybackEngineType {
	override fun promiseSelectedPlaybackEngineType(): Promise<PlaybackEngineType> {
		val preferences = PreferenceManager.getDefaultSharedPreferences(context)

		val playbackEngineTypeString = preferences
			.getString(
				ApplicationConstants.PreferenceConstants.playbackEngine,
				PlaybackEngineType.ExoPlayer.name)

		return if (PlaybackEngineType.values().any { e -> playbackEngineTypeString == e.name }) PlaybackEngineType.valueOf(playbackEngineTypeString!!).toPromise()
		else defaultPlaybackEngineLookup.promiseDefaultEngineType()
			.then { t ->
				preferences
					.edit()
					.putString(
						ApplicationConstants.PreferenceConstants.playbackEngine,
						t.name)
					.apply()

				PlaybackEngineType.valueOf(preferences
					.getString(
						ApplicationConstants.PreferenceConstants.playbackEngine,
						PlaybackEngineType.ExoPlayer.name)!!)
			}
	}
}
