package com.lasthopesoftware.bluewater.client.playback.service.receivers.scrobble

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.duration
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
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
				val artist = fileProperties[NormalizedFileProperties.Artist]
				val name = fileProperties[NormalizedFileProperties.Name]
				val album = fileProperties[NormalizedFileProperties.Album]
				val duration = fileProperties.duration?.standardSeconds

				val scrobbleDroidIntent = scrobbleIntentProvider.provideScrobbleIntent(true)
				scrobbleDroidIntent.putExtra("artist", artist)
				scrobbleDroidIntent.putExtra("album", album)
				scrobbleDroidIntent.putExtra("track", name)
				if (duration != null)
					scrobbleDroidIntent.putExtra("secs", duration)

				fileProperties[NormalizedFileProperties.Track]
					?.takeIf { it.isNotEmpty() }
					?.also {
						scrobbleDroidIntent.putExtra("tracknumber", it.toInt())
					}
				context.sendBroadcast(scrobbleDroidIntent)
			}
	}
}
