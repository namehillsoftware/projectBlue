package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

class PlaybackFileStoppedScrobblerRegistration(private val context: Context) : IConnectionDependentReceiverRegistration {
    override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): ReceiveBroadcastEvents =
		PlaybackFileStoppedScrobbleDroidProxy(ScrobbleIntentProvider.getInstance())

    override fun forIntents(): Collection<IntentFilter> = intents

    private inner class PlaybackFileStoppedScrobbleDroidProxy(private val scrobbleIntentProvider: ScrobbleIntentProvider) :
        ReceiveBroadcastEvents {
        override fun onReceive(intent: Intent) {
            context.sendBroadcast(scrobbleIntentProvider.provideScrobbleIntent(false))
        }
    }

    companion object {
        private val intents by lazy {
			setOf(
				IntentFilter(PlaylistEvents.onPlaylistTrackComplete),
				IntentFilter(PlaylistEvents.onPlaylistStop),
			)
		}
    }
}
