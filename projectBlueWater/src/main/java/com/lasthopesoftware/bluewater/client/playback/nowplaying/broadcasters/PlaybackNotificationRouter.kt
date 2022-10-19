package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise

class PlaybackNotificationRouter(
	private val playbackNotificationBroadcaster: NotifyOfPlaybackEvents,
	private val registerApplicationMessages: RegisterForApplicationMessages,
	private val scopedUrlKeys: ProvideScopedUrlKey,
	private val nowPlayingProvider: GetNowPlayingState,
) :
	(ApplicationMessage) -> Unit,
	AutoCloseable
{
	init {
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.TrackChanged>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackStarted>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackPaused>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackInterrupted>(), this)
		registerApplicationMessages.registerForClass(cls<PlaybackMessage.PlaybackStopped>(), this)
		registerApplicationMessages.registerForClass(cls<FilePropertiesUpdatedMessage>(), this)
	}

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is PlaybackMessage.TrackChanged -> playbackNotificationBroadcaster.notifyPlayingFileUpdated()
			is PlaybackMessage.PlaybackStarted -> playbackNotificationBroadcaster.notifyPlaying()
			is PlaybackMessage.PlaybackPaused -> playbackNotificationBroadcaster.notifyPaused()
			is PlaybackMessage.PlaybackInterrupted -> playbackNotificationBroadcaster.notifyInterrupted()
			is PlaybackMessage.PlaybackStopped -> playbackNotificationBroadcaster.notifyStopped()
			is FilePropertiesUpdatedMessage -> updatePlayingFilePropertiesIfNecessary(message.urlServiceKey)
		}
	}

	override fun close() {
		registerApplicationMessages.unregisterReceiver(this)
	}

	private fun updatePlayingFilePropertiesIfNecessary(urlKeyHolder: UrlKeyHolder<ServiceFile>) {
		promiseNowPlayingUrlKeyHolder()
			.then { nowPlayingUrlKeyHolder ->
				if (urlKeyHolder == nowPlayingUrlKeyHolder) {
					playbackNotificationBroadcaster.notifyPlayingFileUpdated()
				}
			}
	}

	private fun promiseNowPlayingUrlKeyHolder() =
		nowPlayingProvider
			.promiseNowPlaying()
			.eventually { nowPlaying ->
				nowPlaying
					?.playingFile
					?.serviceFile
					?.let { scopedUrlKeys.promiseUrlKey(it) }
					.keepPromise()
			}
}
