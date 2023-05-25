package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.updateIfDifferent
import com.lasthopesoftware.resources.closables.AutoCloseableManager
import java.util.concurrent.atomic.AtomicReference

abstract class PlaybackNotificationRouter(
	registerApplicationMessages: RegisterForApplicationMessages,
) : AutoCloseable
{
	private val autoCloseableManager = AutoCloseableManager()
	private val activeLibraryId = AtomicReference<LibraryId?>(null)

	init {
		autoCloseableManager.manage(registerApplicationMessages.registerReceiver { m: LibraryPlaybackMessage.TrackChanged ->
			updateLibrary(m)
			notifyPlayingFileUpdated()
		})
		autoCloseableManager.manage(registerApplicationMessages.registerReceiver { m: LibraryPlaybackMessage.PlaybackStarted ->
			updateLibrary(m)
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

	protected abstract fun updateLibrary(libraryId: LibraryId)

	protected abstract fun notifyPlaying()

	protected abstract fun notifyPlayingFileUpdated()

	protected abstract fun notifyPaused()

	protected abstract fun notifyInterrupted()

	protected abstract fun notifyStopped()

	override fun close() {
		autoCloseableManager.close()
	}

	private fun updateLibrary(message: LibraryPlaybackMessage) {
		if (activeLibraryId.updateIfDifferent(message.libraryId))
			updateLibrary(message.libraryId)
	}
}
