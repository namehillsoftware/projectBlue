package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.ApplicationDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.ScaledImageProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionDependents
import com.lasthopesoftware.bluewater.shared.images.bytes.GetImageBytes
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.DiskCacheImageAccess
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.ImageCacheKeyLookup
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.LookupImageCacheKey

interface LibraryFilePropertiesDependents {
	val imageBytesProvider: GetImageBytes
	val imageCacheKeyLookup: LookupImageCacheKey
}

class LibraryFilePropertiesDependentsRegistry(
	application: ApplicationDependencies,
	libraryConnectionDependents: LibraryConnectionDependents,
) : LibraryFilePropertiesDependents {

	override val imageCacheKeyLookup by lazy { ImageCacheKeyLookup(libraryConnectionDependents.libraryFilePropertiesProvider) }

	override val imageBytesProvider by lazy {
		val scaledSourceImageProvider = ScaledImageProvider(
			RemoteImageAccess(application.libraryConnectionProvider),
			application.screenDimensions,
		)

		val diskImageProvider = DiskCacheImageAccess(
			scaledSourceImageProvider,
			imageCacheKeyLookup,
			application.imageDiskFileCache
		)

		CachedImageProvider(
			diskImageProvider,
			imageCacheKeyLookup
		)
	}
}
