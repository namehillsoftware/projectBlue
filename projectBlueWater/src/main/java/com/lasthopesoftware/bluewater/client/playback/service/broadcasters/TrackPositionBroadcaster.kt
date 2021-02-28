package com.lasthopesoftware.bluewater.client.playback.service.broadcasters

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import io.reactivex.functions.Consumer
import org.joda.time.Duration

class TrackPositionBroadcaster(private val localBroadcastManager: LocalBroadcastManager, private val playingFile: PlayingFile) : Consumer<Duration> {
	override fun accept(fileProgress: Duration) {
		val trackPositionChangedIntent = Intent(trackPositionUpdate)
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.filePosition, fileProgress.millis)

		playingFile.duration.then { d ->
			trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.fileDuration, d.millis)
			localBroadcastManager.sendBroadcast(trackPositionChangedIntent)
		}
	}

	object TrackPositionChangedParameters {
		private val magicPropertyBuilder = MagicPropertyBuilder(TrackPositionChangedParameters::class.java)
		@JvmField
		val filePosition = magicPropertyBuilder.buildProperty("filePosition")
		@JvmField
		val fileDuration = magicPropertyBuilder.buildProperty("fileDuration")
	}

	companion object {
		private val magicPropertyBuilder = MagicPropertyBuilder(TrackPositionBroadcaster::class.java)
		@JvmField
		val trackPositionUpdate = magicPropertyBuilder.buildProperty("trackPositionUpdate")
	}
}
