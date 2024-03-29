package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideScopedImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.namehillsoftware.handoff.promises.Promise

class MediaItemServiceFileLookup(
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val imageProvider: ProvideScopedImages
) : GetMediaItemsFromServiceFiles {

	override fun promiseMediaItem(serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem> {
		return promiseMediaMetadataWithFileProperties(serviceFile)
			.then { mediaMetadataBuilder ->
				MediaBrowserCompat.MediaItem(
					mediaMetadataBuilder.build().description,
					MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
				)
			}
	}

	override fun promiseMediaItemWithImage(serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem> {
		val promisedImage = imageProvider.promiseFileBitmap(serviceFile)
		return promiseMediaMetadataWithFileProperties(serviceFile)
			.eventually { mediaMetadataBuilder ->
				promisedImage.then { image ->
					mediaMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, image)

					MediaBrowserCompat.MediaItem(
						mediaMetadataBuilder.build().description,
						MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
					)
				}
			}
	}

	private fun promiseMediaMetadataWithFileProperties(serviceFile: ServiceFile) =
		filePropertiesProvider.promiseFileProperties(serviceFile)
			.then { p ->
				MediaMetadataCompat.Builder().apply {
					val artist = p[KnownFileProperties.Artist]
					val name = p[KnownFileProperties.Name]
					val album = p[KnownFileProperties.Album]
					val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(p)

					putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, RemoteBrowserService.serviceFileMediaIdPrefix + RemoteBrowserService.mediaIdDelimiter + p[KnownFileProperties.Key])
					putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
					putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
					putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
					putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

					val trackNumberString = p[KnownFileProperties.Track]
					val trackNumber = trackNumberString?.toLongOrNull()
					if (trackNumber != null) {
						putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
					}
				}
			}
}
