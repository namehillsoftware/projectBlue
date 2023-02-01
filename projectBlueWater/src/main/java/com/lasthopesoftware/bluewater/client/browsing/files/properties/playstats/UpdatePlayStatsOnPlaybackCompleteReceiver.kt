package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats

import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage.TrackCompleted
import com.lasthopesoftware.bluewater.shared.lazyLogger

private val logger by lazyLogger<UpdatePlayStatsOnPlaybackCompleteReceiver>()

class UpdatePlayStatsOnPlaybackCompleteReceiver(private val libraryPlaystatsUpdateSelector: LibraryPlaystatsUpdateSelector) : (TrackCompleted) -> Unit {

	override fun invoke(completedMessage: TrackCompleted) {
		val (libraryId, serviceFile) = completedMessage
		libraryPlaystatsUpdateSelector
			.promisePlaystatsUpdate(libraryId, serviceFile)
			.excuse { e ->
				logger.error("There was an error updating the playstats for the file with key ${completedMessage.completedFile}", e)
			}
	}
}
