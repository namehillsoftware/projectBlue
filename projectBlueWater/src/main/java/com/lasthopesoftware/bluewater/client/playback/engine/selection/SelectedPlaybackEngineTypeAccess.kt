package com.lasthopesoftware.bluewater.client.playback.engine.selection

import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.LookupDefaultPlaybackEngine
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedPlaybackEngineTypeAccess
(
	private val applicationFeatureConfiguration: HoldApplicationFeatureConfiguration,
	private val defaultPlaybackEngineLookup: LookupDefaultPlaybackEngine
) : LookupSelectedPlaybackEngineType {
	private val engineTypes by lazy { PlaybackEngineType.entries.toTypedArray() }

	override fun promiseSelectedPlaybackEngineType(): Promise<PlaybackEngineType> =
		applicationFeatureConfiguration
			.promiseFeatureConfiguration()
			.eventually { s ->
				engineTypes.firstOrNull { e -> e == s.playbackEngineType }
					?.toPromise()
					?: defaultPlaybackEngineLookup.promiseDefaultEngineType()
						.eventually { t ->
							val newConfiguration = s.copy(playbackEngineType = t)
							applicationFeatureConfiguration
								.promiseUpdatedFeatureConfiguration(newConfiguration)
								.then { ns ->
									engineTypes.first { e -> e == ns.playbackEngineType }
								}
						}
					}
}
