package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats

import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.lazyLogger

private val logger by lazyLogger<UpdatePlayStatsOnPlaybackCompleteReceiver>()

class UpdatePlayStatsOnPlaybackCompleteReceiver(private val libraryPlaystatsUpdateSelector: LibraryPlaystatsUpdateSelector) : (LibraryPlaybackMessage.TrackCompleted) -> Unit {

	override fun invoke(completedMessage: LibraryPlaybackMessage.TrackCompleted) {
		val (libraryId, serviceFile) = completedMessage
		libraryPlaystatsUpdateSelector
			.promisePlaystatsUpdate(libraryId, serviceFile)
			.excuse { e ->
				logger.error("There was an error updating the playstats for the file with key ${completedMessage.completedFile}", e)
			}
	}
}
