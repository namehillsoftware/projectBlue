package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats

import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.LibraryPlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.PromiseTracker
import com.lasthopesoftware.resources.closables.PromisingCloseable
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<UpdatePlayStatsOnPlaybackCompletedReceiver>()

class UpdatePlayStatsOnPlaybackCompletedReceiver(
	private val libraryPlaystatsUpdateSelector: LibraryPlaystatsUpdateSelector,
	private val inner: OnPlayingFileChanged
) : OnPlayingFileChanged by inner, PromisingCloseable {

	private val promiseTracker = PromiseTracker()

	override fun onPlayingFileChanged(libraryId: LibraryId, positionedPlayingFile: PositionedPlayingFile?) {
		inner.onPlayingFileChanged(libraryId, positionedPlayingFile)

		val playingFile = positionedPlayingFile?.playingFile ?: return

		if (playingFile is EmptyPlaybackHandler) return

		promiseTracker.track(
			playingFile.promisePlayedFile().eventually {
				val serviceFile = positionedPlayingFile.serviceFile
				val promisedUpdate = libraryPlaystatsUpdateSelector.promisePlaystatsUpdate(libraryId, serviceFile)

				promisedUpdate
					.excuse { e ->
						logger.error("There was an error updating the playstats for the file with key $serviceFile", e)
					}

				promisedUpdate
			}
		)
	}

	override fun promiseClose(): Promise<Unit> = promiseUpdatesFinish()

	fun promiseUpdatesFinish(): Promise<Unit> = promiseTracker.promiseAllConcluded()
}
