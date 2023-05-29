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
	private val autoCloseableManager = AutoCloseableManager()

	init {
		autoCloseableManager.manage(registerApplicationMessages.registerReceiver { _: LibraryPlaybackMessage.TrackChanged ->
			notifyPlayingFileUpdated()
		})
		autoCloseableManager.manage(registerApplicationMessages.registerReceiver { _: PlaybackMessage.PlaybackStarted ->
			notifyPlaying()
		})
		autoCloseableManager.manage(registerApplicationMessages.registerReceiver<PlaybackMessage.PlaybackPaused> {
			notifyPaused()
		})
		autoCloseableManager.manage(registerApplicationMessages.registerReceiver<PlaybackMessage.PlaybackInterrupted> {
			notifyInterrupted()
		})
		autoCloseableManager.manage(registerApplicationMessages.registerReceiver<PlaybackMessage.PlaybackStopped> {
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
