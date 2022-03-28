package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistTrackCompleted
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import org.slf4j.LoggerFactory

class UpdatePlayStatsOnPlaybackCompleteReceiver(private val playstatsUpdateSelector: PlaystatsUpdateSelector) : (ApplicationMessage) -> Unit {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(cls<UpdatePlayStatsOnPlaybackCompleteReceiver>()) }
	}

	override fun invoke(message: ApplicationMessage) {
		val completedMessage = message as? PlaylistTrackCompleted ?: return

		playstatsUpdateSelector
			.promisePlaystatsUpdater()
			.eventually { updater -> updater.promisePlaystatsUpdate(completedMessage.completedFile) }
			.excuse { e ->
				logger.error("There was an error updating the playstats for the file with key ${completedMessage.completedFile}", e)
			}
	}
}
