package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.resources.closables.AutoCloseableManager

class PlaybackNotificationRouter(
	private val playbackNotificationBroadcaster: NotifyOfPlaybackEvents,
	private val registerApplicationMessages: RegisterForApplicationMessages,
	private val urlKeys: ProvideUrlKey,
) : AutoCloseable
{
	private val autoCloseableManager = AutoCloseableManager()
	private var state: Pair<LibraryId, ServiceFile>? = null

	init {
		with (playbackNotificationBroadcaster) {
			autoCloseableManager.manage(registerApplicationMessages.registerReceiver { m: PlaybackMessage.TrackChanged ->
				state = Pair(m.libraryId, m.positionedFile.serviceFile)
				notifyPlayingFileUpdated(m.libraryId, m.positionedFile.serviceFile)
			})
			autoCloseableManager.manage(registerApplicationMessages.registerReceiver { _: PlaybackMessage.PlaybackStarted ->
				notifyPlaying()
			})
			autoCloseableManager.manage(registerApplicationMessages.registerReceiver { _: PlaybackMessage.PlaybackPaused ->
				notifyPaused()
			})
			autoCloseableManager.manage(registerApplicationMessages.registerReceiver { _: PlaybackMessage.PlaybackInterrupted ->
				notifyInterrupted()
			})
			autoCloseableManager.manage(registerApplicationMessages.registerReceiver { _: PlaybackMessage.PlaybackStopped ->
				notifyStopped()
			})
			autoCloseableManager.manage(registerApplicationMessages.registerReceiver { m: FilePropertiesUpdatedMessage ->
				updatePlayingFilePropertiesIfNecessary(m.urlServiceKey)
			})
		}
	}

	override fun close() {
		autoCloseableManager.close()
	}

	private fun updatePlayingFilePropertiesIfNecessary(urlKeyHolder: UrlKeyHolder<ServiceFile>) {
		state?.also { (libraryId, serviceFile) ->
			urlKeys
				.promiseUrlKey(libraryId, serviceFile)
				.then { currentUrlKeyHolder ->
					if (urlKeyHolder == currentUrlKeyHolder)
						playbackNotificationBroadcaster.notifyPlayingFileUpdated(libraryId, serviceFile)
				}
		}
	}
}
