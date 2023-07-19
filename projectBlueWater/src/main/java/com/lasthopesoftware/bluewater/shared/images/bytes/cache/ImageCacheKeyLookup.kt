package com.lasthopesoftware.bluewater.shared.images.bytes.cache

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class ImageCacheKeyLookup(private val cachedFilePropertiesProvider: CachedFilePropertiesProvider) :
	LookupImageCacheKey {
	override fun promiseImageCacheKey(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String> {
		return object : Promise<String>() {
			init {
				val cancellationProxy = CancellationProxy()
				awaitCancellation(cancellationProxy)

				val promisedFileProperties = cachedFilePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
				cancellationProxy.doCancel(promisedFileProperties)

				promisedFileProperties
					.then { fileProperties ->
						// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
						// artists), and then by artist if that field is empty
						var artist = fileProperties[KnownFileProperties.AlbumArtist]
						if (artist == null || artist.isEmpty()) artist = fileProperties[KnownFileProperties.Artist]

						var albumOrTrackName = fileProperties[KnownFileProperties.Album]
						if (albumOrTrackName == null) albumOrTrackName = fileProperties[KnownFileProperties.Name]

						resolve("$artist:$albumOrTrackName")
					}
					.excuse(::reject)
			}
		}
	}
}
