package com.lasthopesoftware.bluewater.client.playback.engine.selection

import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.LookupDefaultPlaybackEngine
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedPlaybackEngineTypeAccess(private val applicationSettings
: HoldApplicationSettings, private val defaultPlaybackEngineLookup: LookupDefaultPlaybackEngine) : LookupSelectedPlaybackEngineType {
	override fun promiseSelectedPlaybackEngineType(): Promise<PlaybackEngineType> =
		applicationSettings
			.promiseApplicationSettings()
			.eventually { s ->
				PlaybackEngineType.values().firstOrNull { e -> e.name == s.playbackEngineType }
					?.toPromise()
					?: defaultPlaybackEngineLookup.promiseDefaultEngineType()
						.eventually { t ->
							s.playbackEngineType = t.name
							applicationSettings
								.promiseUpdatedSettings(s)
								.then { ns ->
									PlaybackEngineType.values().first { e -> e.name == ns.playbackEngineType }
								}
						}
					}
}
