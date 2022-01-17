package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ProvideImages
import com.namehillsoftware.handoff.promises.Promise

class MediaItemServiceFileLookup(
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val imageProvider: ProvideImages
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
					val artist = p[KnownFileProperties.ARTIST]
					val name = p[KnownFileProperties.NAME]
					val album = p[KnownFileProperties.ALBUM]
					val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(p).toLong()

					putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, RemoteBrowserService.serviceFileMediaIdPrefix + RemoteBrowserService.mediaIdDelimiter + p[KnownFileProperties.KEY])
					putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
					putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
					putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
					putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

					val trackNumberString = p[KnownFileProperties.TRACK]
					val trackNumber = trackNumberString?.toLongOrNull()
					if (trackNumber != null) {
						putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
					}
				}
			}
}
