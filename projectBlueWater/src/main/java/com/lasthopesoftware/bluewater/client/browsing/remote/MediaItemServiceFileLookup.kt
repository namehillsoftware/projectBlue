package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class MediaItemServiceFileLookup(
	fileProperties: ProvideScopedFileProperties,
	images: ProvideImages
) : GetMediaItemsFromServiceFiles {


	override fun promiseMediaItem(serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem> {
		val mediaDescription = MediaMetadataCompat.Builder().apply {
			val artist = "Billy Bob"
			val name = "Billy Bob Jr. Jr."
			val album = "Bob's BIIIG Adventure"
			val duration = Duration.standardSeconds(30).millis

			putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, RemoteBrowserService.serviceFileMediaIdPrefix + "14")
			putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
			putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
			putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
			putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

			val trackNumberString = "56"
			val trackNumber = trackNumberString.toLongOrNull()
			if (trackNumber != null) {
				putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
			}
		}.build()

		return Promise(MediaBrowserCompat.MediaItem(
			mediaDescription.description,
			MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
		))
	}
}
