package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import io.reactivex.functions.Consumer
import org.joda.time.Duration

class TrackPositionBroadcaster(
	private val sendApplicationMessages: SendApplicationMessages,
	private val fileProperties: ProvideScopedFileProperties
) {

	fun broadcastProgress(positionedProgressedFile: PositionedProgressedFile) {
		fileProperties
			.promiseFileProperties(positionedProgressedFile.serviceFile)
			.then { p ->
				FilePropertyHelpers.parseDurationIntoMilliseconds(p)
					.takeIf { it > -1 }
					?.let { duration ->
						positionedProgressedFile.progress
							.then { progress ->
								sendApplicationMessages.sendMessage(TrackPositionUpdate(progress, Duration.millis(duration)))
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
