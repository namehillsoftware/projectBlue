package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
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
								sendApplicationMessages.sendMessage(TrackPositionUpdate(progress, Duration.millis(duration.toLong())))
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

	class TrackPositionUpdate(val filePosition: Duration, val fileDuration: Duration) : ApplicationMessage
}
