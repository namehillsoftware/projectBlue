package com.lasthopesoftware.bluewater.settings

import android.content.SharedPreferences
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType
import com.namehillsoftware.handoff.promises.Promise

class ApplicationPreferencesLookup(private val sharedPreferences: SharedPreferences, private val selectedPlaybackEngineLookup: LookupSelectedPlaybackEngineType) : LookupApplicationPreferences {
	override fun promiseApplicationPreferences(): Promise<ApplicationPreferences> {
		return selectedPlaybackEngineLookup.promiseSelectedPlaybackEngineType().then {
			ApplicationPreferences(
				isSyncOnWifiOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false),
				isSyncOnPowerOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false),
				isVolumeLevelingEnabled = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isVolumeLevelingEnabled, false),
				playbackEngine = it,
				skipSilence = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.skipSilence, false))
		}
	}
}
