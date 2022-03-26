package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification

import android.content.Intent
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistStart
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistTrackChange
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages

class PlaybackNotificationRouter(
	private val playbackNotificationBroadcaster: NotifyOfPlaybackEvents,
	private val registerApplicationMessages: RegisterForApplicationMessages
) :
	ReceiveBroadcastEvents,
	(ApplicationMessage) -> Unit,
	AutoCloseable
{
	private val mappedEvents by lazy {
		mapOf(
			Pair(PlaylistEvents.onPlaylistPause) { playbackNotificationBroadcaster.notifyPaused() },
			Pair(PlaylistEvents.onPlaylistInterrupted) { playbackNotificationBroadcaster.notifyInterrupted() },
			Pair(PlaylistEvents.onPlaylistStop) { playbackNotificationBroadcaster.notifyStopped() },
		)
	}

	init {
		registerApplicationMessages.registerForClass(cls<PlaylistTrackChange>(), this)
		registerApplicationMessages.registerForClass(cls<PlaylistStart>(), this)
	}

	fun registerForIntents(): Set<String> = mappedEvents.keys

	override fun onReceive(intent: Intent) {
		intent.action?.let(mappedEvents::get)?.invoke()
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is PlaylistTrackChange -> playbackNotificationBroadcaster.notifyPlayingFileChanged(message.positionedFile.serviceFile)
			is PlaylistStart -> playbackNotificationBroadcaster.notifyPlaying()
		}
	}

	override fun close() {
		registerApplicationMessages.unregisterReceiver(this)
	}
}
