package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.browsing.files.access.CachedItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.ScaledImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.DelegatingFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPollingSessions
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.DiskCacheImageAccess
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.ImageCacheKeyLookup
import com.lasthopesoftware.policies.ratelimiting.RateLimitingExecutionPolicy

open class LibraryConnectionRegistry(
	application: ApplicationDependencies
) : LibraryConnectionDependencies {
	private val libraryFileStringListProvider by lazy { LibraryFileStringListProvider(application.libraryConnectionProvider) }

	private val itemListProvider by lazy {
		ItemStringListProvider(FileListParameters, libraryFileStringListProvider)
	}

	private val guaranteedLibraryConnectionProvider by lazy { GuaranteedLibraryConnectionProvider(application.libraryConnectionProvider) }

	override val connectionAuthenticationChecker by lazy { ConnectionAuthenticationChecker(application.libraryConnectionProvider) }

	override val freshLibraryFileProperties: ProvideFreshLibraryFileProperties by lazy {
		FilePropertiesProvider(guaranteedLibraryConnectionProvider, revisionProvider, FilePropertyCache)
	}

	override val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			application.libraryConnectionProvider,
			FilePropertyCache,
			freshLibraryFileProperties,
		)
	}

	override val imageCacheKeyLookup by lazy { ImageCacheKeyLookup(libraryFilePropertiesProvider) }

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

		val scaledDiskImageProvider = ScaledImageProvider(
			diskImageProvider,
			application.screenDimensions,
		)

		CachedImageProvider(
			scaledDiskImageProvider,
			imageCacheKeyLookup
		)
	}

	override val urlKeyProvider by lazy { UrlKeyProvider(application.libraryConnectionProvider) }

	override val revisionProvider by lazy { LibraryRevisionProvider(application.libraryConnectionProvider) }

	override val filePropertiesStorage by lazy {
		FilePropertyStorage(
			application.libraryConnectionProvider,
			connectionAuthenticationChecker,
			revisionProvider,
			FilePropertyCache,
			application.sendApplicationMessages
		)
	}

	override val itemProvider: ProvideItems by lazy {
		CachedItemProvider(
			ItemProvider(guaranteedLibraryConnectionProvider),
			revisionProvider
		)
	}

	override val itemStringListProvider by lazy {
		ItemStringListProvider(
			FileListParameters,
			libraryFileStringListProvider,
		)
	}

	override val itemFileProvider: ProvideItemFiles by lazy {
		CachedItemFileProvider(
			ItemFileProvider(itemStringListProvider),
			revisionProvider
		)
	}

	override val libraryFilesProvider by lazy {
		LibraryFileProvider(libraryFileStringListProvider)
	}

	override val playbackLibraryItems by lazy { ItemPlayback(itemListProvider, application.playbackServiceController) }

	override val pollForConnections by lazy {
		LibraryConnectionPollingSessions(LibraryConnectionPoller(application.connectionSessions))
	}
}

open class RateLimitedFilePropertiesDependencies(
    application: ApplicationDependencies,
    private val filePropertiesRatePolicy: RateLimitingExecutionPolicy,
) : LibraryConnectionRegistry(application) {
	override val freshLibraryFileProperties by lazy {
		DelegatingFilePropertiesProvider(
			super.freshLibraryFileProperties,
			filePropertiesRatePolicy,
		)
	}
}
