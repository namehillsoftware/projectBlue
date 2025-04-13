package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.durationInMs
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.resources.bitmaps.ProduceBitmaps
import com.namehillsoftware.handoff.promises.Promise

class MediaItemServiceFileLookup(
	private val filePropertiesProvider: ProvideLibraryFileProperties,
	private val imageProvider: GetImageBytes,
	private val bitmapProducer: ProduceBitmaps,
) : GetMediaItemsFromServiceFiles {

	override fun promiseMediaItem(libraryId: LibraryId, serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem> {
		return promiseMediaMetadataWithFileProperties(libraryId, serviceFile)
			.then { mediaMetadataBuilder ->
				MediaBrowserCompat.MediaItem(
					mediaMetadataBuilder.build().description,
					MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
				)
			}
	}

	override fun promiseMediaItemWithImage(libraryId: LibraryId, serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem> {
		val promisedImage = imageProvider.promiseImageBytes(libraryId, serviceFile).eventually(bitmapProducer::promiseBitmap)
		return promiseMediaMetadataWithFileProperties(libraryId, serviceFile)
			.eventually { mediaMetadataBuilder ->
				promisedImage.then { bitmap ->
					mediaMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
					mediaMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
					mediaMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)

					MediaBrowserCompat.MediaItem(
						mediaMetadataBuilder.build().description,
						MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
					)
				}
			}
	}

	private fun promiseMediaMetadataWithFileProperties(libraryId: LibraryId, serviceFile: ServiceFile) =
		filePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
			.then { p ->
				MediaMetadataCompat.Builder().apply {
					val artist = p[NormalizedFileProperties.Artist]
					val name = p[NormalizedFileProperties.Name]
					val album = p[NormalizedFileProperties.Album]
					val duration = p.durationInMs ?: -1

					putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, RemoteBrowserService.serviceFileMediaIdPrefix + RemoteBrowserService.mediaIdDelimiter + p[NormalizedFileProperties.Key])
					putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
					putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
					putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
					putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

					val trackNumberString = p[NormalizedFileProperties.Track]
					val trackNumber = trackNumberString?.toLongOrNull()
					if (trackNumber != null) {
						putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
					}
				}
			}
}
