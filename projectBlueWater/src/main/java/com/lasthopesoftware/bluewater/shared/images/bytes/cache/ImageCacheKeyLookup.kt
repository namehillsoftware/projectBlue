package com.lasthopesoftware.bluewater.shared.images.bytes.cache

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ImageCacheKeyLookup(private val cachedFilePropertiesProvider: CachedFilePropertiesProvider) :
	LookupImageCacheKey, ImmediateResponse<Map<String, String>, String> {
	override fun promiseImageCacheKey(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String> =
		object : Promise.Proxy<String>() {
			init {
				val promisedFileProperties = cachedFilePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
				doCancel(promisedFileProperties)

				proxy(
					promisedFileProperties.then(this@ImageCacheKeyLookup)
				)
			}
		}

	override fun respond(fileProperties: Map<String, String>): String {
		// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
		// artists), and then by artist if that field is empty
		var artist = fileProperties[KnownFileProperties.AlbumArtist]
		if (artist.isNullOrEmpty()) artist = fileProperties[KnownFileProperties.Artist]

		var albumOrTrackName = fileProperties[KnownFileProperties.Album]
		if (albumOrTrackName == null) albumOrTrackName = fileProperties[KnownFileProperties.Name]

		return "$artist:$albumOrTrackName"
	}
}
