package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents

class PlaybackFileStartedScrobblerRegistration : IConnectionDependentReceiverRegistration {

	companion object {
		private val intents: Collection<IntentFilter> =
			setOf(IntentFilter(PlaylistEvents.onPlaylistTrackStart))
	}

    override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): BroadcastReceiver {
        val filePropertiesProvider = ScopedCachedFilePropertiesProvider(
            connectionProvider,
            FilePropertyCache.getInstance(),
            ScopedFilePropertiesProvider(
                connectionProvider,
				ScopedRevisionProvider(connectionProvider),
                FilePropertyCache.getInstance()
            )
        )
        return PlaybackFileChangedScrobbleDroidProxy(
            filePropertiesProvider,
            ScrobbleIntentProvider.getInstance()
        )
    }

    override fun forIntents(): Collection<IntentFilter> = intents

    private class PlaybackFileChangedScrobbleDroidProxy(
        private val scopedCachedFilePropertiesProvider: ScopedCachedFilePropertiesProvider,
        private val scrobbleIntentProvider: ScrobbleIntentProvider
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
            if (fileKey < 0) return
            scopedCachedFilePropertiesProvider
                .promiseFileProperties(ServiceFile(fileKey))
                .then { fileProperties ->
                    val artist = fileProperties[KnownFileProperties.ARTIST]
                    val name = fileProperties[KnownFileProperties.NAME]
                    val album = fileProperties[KnownFileProperties.ALBUM]
                    val duration =
                        FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties).toLong()

                    val scrobbleDroidIntent = scrobbleIntentProvider.provideScrobbleIntent(true)
                    scrobbleDroidIntent.putExtra("artist", artist)
                    scrobbleDroidIntent.putExtra("album", album)
                    scrobbleDroidIntent.putExtra("track", name)
                    scrobbleDroidIntent.putExtra("secs", (duration / 1000).toInt())

					fileProperties[KnownFileProperties.TRACK]
						?.takeIf { it.isNotEmpty() }
						?.also {
							scrobbleDroidIntent.putExtra("tracknumber", it.toInt())
						}
                    context.sendBroadcast(scrobbleDroidIntent)
                }
        }
    }
}
