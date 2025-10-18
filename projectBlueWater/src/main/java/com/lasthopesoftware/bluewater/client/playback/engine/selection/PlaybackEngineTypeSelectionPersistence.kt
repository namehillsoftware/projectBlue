package com.lasthopesoftware.bluewater.client.playback.engine.selection

import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class PlaybackEngineTypeSelectionPersistence(private val applicationFeatureConfiguration: HoldApplicationFeatureConfiguration, private val playbackEngineTypeChangedBroadcaster: PlaybackEngineTypeChangedBroadcaster) : SelectPlaybackEngineType {
	override fun selectPlaybackEngine(playbackEngineType: PlaybackEngineType): Promise<Unit> =
		applicationFeatureConfiguration.promiseFeatureConfiguration()
			.eventually { s ->
				if (s.playbackEngineType != playbackEngineType) {
					playbackEngineTypeChangedBroadcaster.broadcastPlaybackEngineTypeChanged(playbackEngineType)
				}

				val newConfiguration = s.copy(playbackEngineType = playbackEngineType)
				applicationFeatureConfiguration.promiseUpdatedFeatureConfiguration(newConfiguration)
			}
			.unitResponse()
}
