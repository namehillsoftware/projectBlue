package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.UpdatePlayStatsOnPlaybackCompleteReceiver
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import org.slf4j.LoggerFactory

class UpdatePlayStatsOnPlaybackCompleteReceiver(private val playstatsUpdateSelector: PlaystatsUpdateSelector) : ReceiveBroadcastEvents {

	companion object {
		private val logger = LoggerFactory.getLogger(UpdatePlayStatsOnPlaybackCompleteReceiver::class.java)
	}

    override fun onReceive(intent: Intent) {
        val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
        if (fileKey < 0) return

        playstatsUpdateSelector
            .promisePlaystatsUpdater()
            .eventually { updater -> updater.promisePlaystatsUpdate(ServiceFile(fileKey)) }
            .excuse { e ->
                logger.error("There was an error updating the playstats for the file with key $fileKey", e)
            }
    }
}
