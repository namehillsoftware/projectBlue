package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage

class PlaybackFileStartedScrobbleDroidProxy(
	private val context: Context,
	private val libraryFilePropertiesProvider: ProvideLibraryFileProperties,
	private val scrobbleIntentProvider: ScrobbleIntentProvider
) : (LibraryPlaybackMessage.TrackStarted) -> Unit {

	override fun invoke(trackStarted: LibraryPlaybackMessage.TrackStarted) {
		libraryFilePropertiesProvider
			.promiseFileProperties(trackStarted.libraryId, trackStarted.startedFile)
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
