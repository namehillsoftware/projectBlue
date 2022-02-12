package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.pebble

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

private const val PEBBLE_NOTIFY_INTENT = "com.getpebble.action.NOW_PLAYING"

internal class PebbleFileChangedProxy(private val scopedCachedFilePropertiesProvider: ScopedCachedFilePropertiesProvider) :
    ReceiveBroadcastEvents {
    override fun onReceive(context: Context, intent: Intent) {
        val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
        if (fileKey < 0) return
        scopedCachedFilePropertiesProvider
            .promiseFileProperties(ServiceFile(fileKey))
            .then { fileProperties ->
				val artist = fileProperties[KnownFileProperties.ARTIST]
				val name = fileProperties[KnownFileProperties.NAME]
				val album = fileProperties[KnownFileProperties.ALBUM]
				val pebbleIntent = Intent(PEBBLE_NOTIFY_INTENT)
				pebbleIntent.putExtra("artist", artist)
				pebbleIntent.putExtra("album", album)
				pebbleIntent.putExtra("track", name)
				context.sendBroadcast(pebbleIntent)
			}
	}
}
