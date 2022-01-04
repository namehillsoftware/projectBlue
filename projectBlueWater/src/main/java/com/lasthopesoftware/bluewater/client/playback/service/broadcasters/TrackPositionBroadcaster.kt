package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import android.content.Intent
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import io.reactivex.functions.Consumer
import org.joda.time.Duration

class TrackPositionBroadcaster(private val sendMessages: SendMessages) {

	fun updateProgress(playingFile: PlayingFile) {
		val promisedProgress = playingFile.progress
		playingFile.duration.then { d ->
			promisedProgress.then { p ->
				val trackPositionChangedIntent = Intent(trackPositionUpdate)
				trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.filePosition, p.millis)
				trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.fileDuration, d.millis)
				sendMessages.sendBroadcast(trackPositionChangedIntent)
			}
		}
	}

	fun observeUpdates(playingFile: PlayingFile): Consumer<Duration> = TrackPositionObserver(playingFile)

	private inner class TrackPositionObserver(private val playingFile: PlayingFile): Consumer<Duration> {
		override fun accept(fileProgress: Duration) {
			val trackPositionChangedIntent = Intent(trackPositionUpdate)
			trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.filePosition, fileProgress.millis)

			playingFile.duration.then { d ->
				trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.fileDuration, d.millis)
				sendMessages.sendBroadcast(trackPositionChangedIntent)
			}
		}
	}

	object TrackPositionChangedParameters {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(TrackPositionChangedParameters::class.java) }

		val filePosition by lazy { magicPropertyBuilder.buildProperty("filePosition") }
		val fileDuration by lazy { magicPropertyBuilder.buildProperty("fileDuration") }
	}

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(TrackPositionBroadcaster::class.java) }
		val trackPositionUpdate by lazy { magicPropertyBuilder.buildProperty("trackPositionUpdate") }
	}
}
