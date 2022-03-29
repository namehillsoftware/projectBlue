package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Context
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.receivers.RegisterReceiverForEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

class PlaybackFileStoppedScrobblerRegistration(private val context: Context) : RegisterReceiverForEvents {
	override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): (ApplicationMessage) -> Unit =
		PlaybackFileStoppedScrobbleDroidProxy(ScrobbleIntentProvider.getInstance())

	@Suppress("UNCHECKED_CAST")
	override fun forClasses(): Collection<Class<ApplicationMessage>> = classes as Collection<Class<ApplicationMessage>>

	private inner class PlaybackFileStoppedScrobbleDroidProxy(private val scrobbleIntentProvider: ScrobbleIntentProvider) :
		(ApplicationMessage) -> Unit {

		override fun invoke(p1: ApplicationMessage) {
			context.sendBroadcast(scrobbleIntentProvider.provideScrobbleIntent(false))
		}
	}

	companion object {
		private val classes by lazy {
			setOf<Class<*>>(cls<PlaybackMessage.PlaybackStopped>(), cls<PlaybackMessage.TrackCompleted>())
		}
	}
}
