package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedCachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.receivers.RegisterReceiverForEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

class PlaybackFileStartedScrobblerRegistration(private val context: Context) : RegisterReceiverForEvents {

	companion object {
		private val classes by lazy { setOf<Class<out ApplicationMessage>>(cls<PlaybackMessage.TrackStarted>()) }
	}

	override fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): (ApplicationMessage) -> Unit {
		val filePropertiesProvider = ScopedCachedFilePropertiesProvider(
			connectionProvider,
			FilePropertyCache,
			ScopedFilePropertiesProvider(
				connectionProvider,
				ScopedRevisionProvider(connectionProvider),
				FilePropertyCache
			)
		)
		return PlaybackFileChangedScrobbleDroidProxy(
			filePropertiesProvider,
			ScrobbleIntentProvider.getInstance()
		)
	}

	override fun forClasses(): Collection<Class<out ApplicationMessage>> = classes

	private inner class PlaybackFileChangedScrobbleDroidProxy(
		private val scopedCachedFilePropertiesProvider: ScopedCachedFilePropertiesProvider,
		private val scrobbleIntentProvider: ScrobbleIntentProvider
	) : (ApplicationMessage) -> Unit {

		override fun invoke(message: ApplicationMessage) {
			val trackStarted = message as? PlaybackMessage.TrackStarted ?: return

			scopedCachedFilePropertiesProvider
				.promiseFileProperties(trackStarted.startedFile)
				.then { fileProperties ->
					val artist = fileProperties[KnownFileProperties.Artist]
					val name = fileProperties[KnownFileProperties.Name]
					val album = fileProperties[KnownFileProperties.Album]
					val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)

					val scrobbleDroidIntent = scrobbleIntentProvider.provideScrobbleIntent(true)
					scrobbleDroidIntent.putExtra("artist", artist)
					scrobbleDroidIntent.putExtra("album", album)
					scrobbleDroidIntent.putExtra("track", name)
					scrobbleDroidIntent.putExtra("secs", (duration / 1000).toInt())

					fileProperties[KnownFileProperties.Track]
						?.takeIf { it.isNotEmpty() }
						?.also {
							scrobbleDroidIntent.putExtra("tracknumber", it.toInt())
						}
					context.sendBroadcast(scrobbleDroidIntent)
				}
		}
	}
}
