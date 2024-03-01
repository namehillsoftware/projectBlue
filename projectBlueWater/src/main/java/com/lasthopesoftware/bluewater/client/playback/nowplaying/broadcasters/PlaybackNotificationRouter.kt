package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.resources.closables.AutoCloseableManager

abstract class PlaybackNotificationRouter(
	registerApplicationMessages: RegisterForApplicationMessages,
) : AutoCloseable
{
	private val autoCloseableManager = AutoCloseableManager().apply {
		manage(registerApplicationMessages.registerReceiver<LibraryPlaybackMessage.TrackChanged> {
			notifyPlayingFileUpdated()
		})
		manage(registerApplicationMessages.registerReceiver<PlaybackMessage.PlaybackStarted> {
			notifyPlaying()
		})
		manage(registerApplicationMessages.registerReceiver<PlaybackMessage.PlaybackPaused> {
			notifyPaused()
		})
		manage(registerApplicationMessages.registerReceiver<PlaybackMessage.PlaybackInterrupted> {
			notifyInterrupted()
		})
		manage(registerApplicationMessages.registerReceiver<PlaybackMessage.PlaybackStopped> {
			notifyStopped()
		})
	}

	protected abstract fun notifyPlaying()

	protected abstract fun notifyPlayingFileUpdated()

	protected abstract fun notifyPaused()

	protected abstract fun notifyInterrupted()

	protected abstract fun notifyStopped()

	override fun close() {
		autoCloseableManager.close()
	}
}
