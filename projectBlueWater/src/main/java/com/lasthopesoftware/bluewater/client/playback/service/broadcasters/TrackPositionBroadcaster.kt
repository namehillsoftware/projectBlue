package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.duration
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import io.reactivex.rxjava3.functions.Consumer
import org.joda.time.Duration

class TrackPositionBroadcaster(
	private val sendApplicationMessages: SendApplicationMessages,
	private val fileProperties: ProvideLibraryFileProperties
) {

	fun broadcastProgress(libraryId: LibraryId, positionedProgressedFile: PositionedProgressedFile) {
		fileProperties
			.promiseFileProperties(libraryId, positionedProgressedFile.serviceFile)
			.then { p ->
				p.duration?.let { duration ->
					positionedProgressedFile.progress
						.then { progress ->
							sendApplicationMessages.sendMessage(TrackPositionUpdate(progress, duration))
						}
				}
			}
	}

	fun observeUpdates(playingFile: PlayingFile): Consumer<Duration> = TrackPositionObserver(playingFile)

	private inner class TrackPositionObserver(private val playingFile: PlayingFile): Consumer<Duration> {
		override fun accept(fileProgress: Duration) {
			playingFile.duration.then { d ->
				sendApplicationMessages.sendMessage(TrackPositionUpdate(fileProgress, d))
			}
		}
	}
}
