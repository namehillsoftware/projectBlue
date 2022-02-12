package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.receivers.IConnectionDependentReceiverRegistration
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

class PlaybackFileStoppedScrobblerRegistration : IConnectionDependentReceiverRegistration {
    override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): ReceiveBroadcastEvents {
        return PlaybackFileStoppedScrobbleDroidProxy(ScrobbleIntentProvider.getInstance())
    }

    override fun forIntents(): Collection<IntentFilter> {
        return intents
    }

    private class PlaybackFileStoppedScrobbleDroidProxy(private val scrobbleIntentProvider: ScrobbleIntentProvider) :
        ReceiveBroadcastEvents {
        override fun onReceive(context: Context, intent: Intent) {
            context.sendBroadcast(scrobbleIntentProvider.provideScrobbleIntent(false))
        }
    }

    companion object {
        private val intents by lazy {
			listOf(
				IntentFilter(PlaylistEvents.onPlaylistTrackComplete),
				IntentFilter(PlaylistEvents.onPlaylistStop),
				IntentFilter(PlaylistEvents.onPlaylistPause)
			)
		}
    }
}
