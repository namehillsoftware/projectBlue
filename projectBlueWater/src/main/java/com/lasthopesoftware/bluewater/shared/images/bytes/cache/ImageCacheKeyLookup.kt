package com.lasthopesoftware.bluewater.shared.images.bytes.cache

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class ImageCacheKeyLookup(private val cachedFilePropertiesProvider: CachedFilePropertiesProvider) : LookupImageCacheKey {
	override fun promiseImageCacheKey(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String> =
		object : Promise.Proxy<String>(), ImmediateResponse<Map<String, String>, String> {
			init {
				val promisedFileProperties = cachedFilePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
				doCancel(promisedFileProperties)

				proxy(
					promisedFileProperties.then(this)
				)
			}

			override fun respond(fileProperties: Map<String, String>): String {
				// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
				// artists), and then by artist if that field is empty
				var artist = fileProperties[KnownFileProperties.AlbumArtist]
				if (artist.isNullOrEmpty()) artist = fileProperties[KnownFileProperties.Artist]

				var albumOrTrackName = fileProperties[KnownFileProperties.Album]
				if (albumOrTrackName == null) albumOrTrackName = fileProperties[KnownFileProperties.Name]

				return "$libraryId:$artist:$albumOrTrackName"
			}
		}

	override fun promiseImageCacheKey(libraryId: LibraryId, itemId: ItemId): Promise<String> = "$libraryId:$itemId".toPromise()
}
