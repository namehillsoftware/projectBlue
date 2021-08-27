package com.lasthopesoftware.bluewater.client.playback.engine.selection

import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class PlaybackEngineTypeSelectionPersistence(private val applicationSettings: HoldApplicationSettings, private val playbackEngineTypeChangedBroadcaster: PlaybackEngineTypeChangedBroadcaster) : SelectPlaybackEngineType {
	override fun selectPlaybackEngine(playbackEngineType: PlaybackEngineType): Promise<Unit> =
		applicationSettings.promiseApplicationSettings()
			.eventually { s ->
				s.playbackEngineType = playbackEngineType.name
				applicationSettings.promiseUpdatedSettings(s)
			}
			.unitResponse()
}
