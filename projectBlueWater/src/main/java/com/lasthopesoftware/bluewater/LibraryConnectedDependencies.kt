package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.client.browsing.files.access.CachedItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.DelegatingItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.ScaledImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.DelegatingFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.access.DelegatingItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPollingSessions
import com.lasthopesoftware.bluewater.shared.images.bytes.RemoteImageAccess
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.DiskCacheImageAccess
import com.lasthopesoftware.bluewater.shared.images.bytes.cache.ImageCacheKeyLookup
import com.lasthopesoftware.policies.retries.RateLimitingExecutionPolicy
import com.lasthopesoftware.policies.retries.RetryExecutionPolicy

class LibraryConnectedDependencies(
	application: ApplicationDependencies
) : LibraryConnectionDependencies {
	private val libraryFileStringListProvider by lazy { LibraryFileStringListProvider(application.libraryConnectionProvider) }

	private val itemListProvider by lazy {
		ItemStringListProvider(FileListParameters, libraryFileStringListProvider)
	}

	private val connectionLostRetryPolicy by lazy { RetryExecutionPolicy(ConnectionLostRetryHandler) }

	private val singleRatePolicy by lazy { RateLimitingExecutionPolicy(1) }

	private val remoteImageAccess by lazy { RemoteImageAccess(application.libraryConnectionProvider) }

	private val guaranteedLibraryConnectionProvider by lazy { GuaranteedLibraryConnectionProvider(application.libraryConnectionProvider) }

	override val connectionAuthenticationChecker by lazy { ConnectionAuthenticationChecker(application.libraryConnectionProvider) }

	override val freshLibraryFileProperties by lazy {
		DelegatingFilePropertiesProvider(
			DelegatingFilePropertiesProvider(
				FilePropertiesProvider(
					guaranteedLibraryConnectionProvider,
					revisionProvider,
					FilePropertyCache,
				),
				connectionLostRetryPolicy,
			),
			singleRatePolicy,
		)
	}

	override val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			application.libraryConnectionProvider,
			FilePropertyCache,
			freshLibraryFileProperties,
		)
	}

	override val imageCacheKeyLookup by lazy { ImageCacheKeyLookup(libraryFilePropertiesProvider) }

	override val rawImageProvider by lazy {
		val diskImageProvider = DiskCacheImageAccess(
			remoteImageAccess,
			imageCacheKeyLookup,
			application.imageDiskFileCache
		)

		val scaledImageProvider = ScaledImageProvider(
			diskImageProvider,
			application.screenDimensions,
		)

		CachedImageProvider(
			scaledImageProvider,
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

	override val itemProvider by lazy {
		DelegatingItemProvider(
			CachedItemProvider(
				ItemProvider(guaranteedLibraryConnectionProvider),
				revisionProvider
			),
			connectionLostRetryPolicy
		)
	}

	override val itemFileProvider by lazy {
		DelegatingItemFileProvider(
			CachedItemFileProvider(
				ItemFileProvider(
					ItemStringListProvider(
						FileListParameters,
						libraryFileStringListProvider,
					)
				),
				revisionProvider
			),
			connectionLostRetryPolicy,
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
