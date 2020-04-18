package com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class ImageCacheKeyLookup(private val cachedFilePropertiesProvider: CachedFilePropertiesProvider) : LookupImageCacheKey {
	override fun promiseImageCacheKey(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String> {
		return object : Promise<String>() {
			init {
				val cancellationProxy = CancellationProxy()
				respondToCancellation(cancellationProxy)

				val promisedFileProperties = cachedFilePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
				cancellationProxy.doCancel(promisedFileProperties)

				promisedFileProperties
					.then { fileProperties ->
						// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
						// artists), and then by artist if that field is empty
						var artist = fileProperties[KnownFileProperties.ALBUM_ARTIST]
						if (artist == null || artist.isEmpty()) artist = fileProperties[KnownFileProperties.ARTIST]

						var albumOrTrackName = fileProperties[KnownFileProperties.ALBUM]
						if (albumOrTrackName == null) albumOrTrackName = fileProperties[KnownFileProperties.NAME]

						resolve("$artist:$albumOrTrackName")
					}
					.excuse { reject(it) }
			}
		}
	}
}
