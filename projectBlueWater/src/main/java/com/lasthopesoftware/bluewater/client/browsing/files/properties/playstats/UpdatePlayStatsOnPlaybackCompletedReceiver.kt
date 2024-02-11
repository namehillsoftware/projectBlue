package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats

import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.shared.lazyLogger

private val logger by lazyLogger<UpdatePlayStatsOnPlaybackCompletedReceiver>()

class UpdatePlayStatsOnPlaybackCompletedReceiver(
	private val libraryPlaystatsUpdateSelector: LibraryPlaystatsUpdateSelector,
	private val inner: OnPlayingFileChanged
) : OnPlayingFileChanged by inner {

	override fun onPlayingFileChanged(libraryId: LibraryId, positionedPlayingFile: PositionedPlayingFile?) {
		val playingFile = positionedPlayingFile?.playingFile ?: return

		if (playingFile is EmptyPlaybackHandler) return

		playingFile.promisePlayedFile().then { pf ->
			val serviceFile = positionedPlayingFile.serviceFile
			libraryPlaystatsUpdateSelector
				.promisePlaystatsUpdate(libraryId, serviceFile)
				.excuse { e ->
					logger.error("There was an error updating the playstats for the file with key $serviceFile", e)
				}
		}

		inner.onPlayingFileChanged(libraryId, positionedPlayingFile)
	}
}
