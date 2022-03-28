package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.receivers.RegisterReceiverForEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackStopped
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

class PlaybackFileStoppedScrobblerRegistration(private val context: Context) : RegisterReceiverForEvents {
    override fun registerBroadcastEventsWithConnectionProvider(connectionProvider: IConnectionProvider): ReceiveBroadcastEvents =
		PlaybackFileStoppedScrobbleDroidProxy(ScrobbleIntentProvider.getInstance())

	override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): (ApplicationMessage) -> Unit =
		PlaybackFileStoppedScrobbleDroidProxy(ScrobbleIntentProvider.getInstance())

	override fun forIntents(): Collection<IntentFilter> = intents

	@Suppress("UNCHECKED_CAST")
	override fun forClasses(): Collection<Class<ApplicationMessage>> = classes as Collection<Class<ApplicationMessage>>

	private inner class PlaybackFileStoppedScrobbleDroidProxy(private val scrobbleIntentProvider: ScrobbleIntentProvider) :
        ReceiveBroadcastEvents, (ApplicationMessage) -> Unit {
        override fun onReceive(intent: Intent) {
            context.sendBroadcast(scrobbleIntentProvider.provideScrobbleIntent(false))
        }

		override fun invoke(p1: ApplicationMessage) {
			context.sendBroadcast(scrobbleIntentProvider.provideScrobbleIntent(false))
		}
	}

    companion object {
        private val intents by lazy {
			setOf(
				IntentFilter(PlaylistEvents.onPlaylistTrackComplete),
			)
		}

		private val classes by lazy {
			setOf<Class<*>>(cls<PlaybackStopped>())
		}
    }
}
