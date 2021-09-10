package com.lasthopesoftware.bluewater.client.playback.engine.selection

import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.LookupDefaultPlaybackEngine
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedPlaybackEngineTypeAccess
(
	private val applicationSettings: HoldApplicationSettings,
	private val defaultPlaybackEngineLookup: LookupDefaultPlaybackEngine
) : LookupSelectedPlaybackEngineType {
	private val engineTypes by lazy { PlaybackEngineType.values() }

	override fun promiseSelectedPlaybackEngineType(): Promise<PlaybackEngineType> =
		applicationSettings
			.promiseApplicationSettings()
			.eventually { s ->
				engineTypes.firstOrNull { e -> e.name == s.playbackEngineTypeName }
					?.toPromise()
					?: defaultPlaybackEngineLookup.promiseDefaultEngineType()
						.eventually { t ->
							s.playbackEngineTypeName = t.name
							applicationSettings
								.promiseUpdatedSettings(s)
								.then { ns ->
									engineTypes.first { e -> e.name == ns.playbackEngineTypeName }
								}
						}
					}
}
