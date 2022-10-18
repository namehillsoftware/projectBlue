package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages

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

	@Volatile
	private var activeNowPlayingUrlKey: UrlKeyHolder<ServiceFile>? = null

	override fun invoke(message: ApplicationMessage) {
		when (message) {
			is PlaybackMessage.TrackChanged -> updatePlayingFilePropertiesIfNecessary()
			is PlaybackMessage.PlaybackStarted -> playbackNotificationBroadcaster.notifyPlaying()
			is PlaybackMessage.PlaybackPaused -> playbackNotificationBroadcaster.notifyPaused()
			is PlaybackMessage.PlaybackInterrupted -> playbackNotificationBroadcaster.notifyInterrupted()
			is PlaybackMessage.PlaybackStopped -> playbackNotificationBroadcaster.notifyStopped()
			is FilePropertiesUpdatedMessage -> updatePlayingFilePropertiesIfNecessary()
		}
	}

	override fun close() {
		registerApplicationMessages.unregisterReceiver(this)
	}

	private fun updatePlayingFilePropertiesIfNecessary() {
		nowPlayingProvider
			.promiseNowPlaying()
			.then { nowPlaying ->
				nowPlaying?.playingFile?.serviceFile?.also {
					scopedUrlKeys
						.promiseUrlKey(it)
						.then { urlKeyHolder ->
							if (activeNowPlayingUrlKey != urlKeyHolder) {
								activeNowPlayingUrlKey = urlKeyHolder
								playbackNotificationBroadcaster.notifyPlayingFileUpdated()
							}
						}
				}
			}
	}
}
