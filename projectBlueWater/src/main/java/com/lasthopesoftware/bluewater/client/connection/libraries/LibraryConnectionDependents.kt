package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.ApplicationDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.access.CachedItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.DelegatingItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.DelegatingFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideFreshLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.access.DelegatingItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.items.list.PlaybackLibraryItems
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPollingSessions
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.policies.ratelimiting.RateLimitingExecutionPolicy
import com.lasthopesoftware.policies.retries.RecursivePromiseRetryHandler
import com.lasthopesoftware.policies.retries.RetryExecutionPolicy

interface LibraryConnectionDependents {
	val urlKeyProvider: UrlKeyProvider
	val revisionProvider: CheckRevisions
	val filePropertiesStorage: FilePropertyStorage
	val itemProvider: ProvideItems
	val itemFileProvider: ProvideItemFiles
	val libraryFilesProvider: LibraryFileProvider
	val playbackLibraryItems: PlaybackLibraryItems
	val pollForConnections: PollForLibraryConnections
	val libraryFilePropertiesProvider: CachedFilePropertiesProvider
	val freshLibraryFileProperties: ProvideFreshLibraryFileProperties
	val connectionAuthenticationChecker: ConnectionAuthenticationChecker
    val itemStringListProvider: ProvideFileStringListForItem
}

class LibraryConnectionRegistry(application: ApplicationDependencies) : LibraryConnectionDependents {
	private val guaranteedLibraryConnectionProvider by lazy { GuaranteedLibraryConnectionProvider(application.libraryConnectionProvider) }

	override val connectionAuthenticationChecker by lazy { ConnectionAuthenticationChecker(application.libraryConnectionProvider) }

	override val freshLibraryFileProperties: ProvideFreshLibraryFileProperties by lazy {
		FilePropertiesProvider(guaranteedLibraryConnectionProvider, revisionProvider, FilePropertyCache)
	}

	override val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			urlKeyProvider,
			FilePropertyCache,
			freshLibraryFileProperties,
		)
	}

	override val urlKeyProvider by lazy { UrlKeyProvider(application.libraryConnectionProvider) }

	override val revisionProvider by lazy { LibraryRevisionProvider(application.libraryConnectionProvider) }

	override val filePropertiesStorage by lazy {
		FilePropertyStorage(
			application.libraryConnectionProvider,
			urlKeyProvider,
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

	override val itemStringListProvider by lazy { ItemStringListProvider(application.libraryConnectionProvider) }

	override val itemFileProvider: ProvideItemFiles by lazy {
		CachedItemFileProvider(
			ItemFileProvider(itemStringListProvider),
			revisionProvider
		)
	}

	override val libraryFilesProvider by lazy { LibraryFileProvider(application.libraryConnectionProvider) }

	override val playbackLibraryItems by lazy { ItemPlayback(itemStringListProvider, application.playbackServiceController) }

	override val pollForConnections by lazy {
		LibraryConnectionPollingSessions(LibraryConnectionPoller(application.connectionSessions))
	}
}

class RetryingLibraryConnectionRegistry(inner: LibraryConnectionDependents) : LibraryConnectionDependents by inner {
	private val connectionLostRetryPolicy by lazy {
		RetryExecutionPolicy(ConnectionLostRetryHandler(RecursivePromiseRetryHandler))
	}

	override val itemProvider by lazy {
		DelegatingItemProvider(
			inner.itemProvider,
			connectionLostRetryPolicy
		)
	}

	override val itemFileProvider by lazy {
		DelegatingItemFileProvider(
			inner.itemFileProvider,
			connectionLostRetryPolicy,
		)
	}

	override val freshLibraryFileProperties by lazy {
		DelegatingFilePropertiesProvider(
			inner.freshLibraryFileProperties,
			connectionLostRetryPolicy,
		)
	}

	override val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			urlKeyProvider,
			FilePropertyCache,
			freshLibraryFileProperties,
		)
	}
}

class RateLimitedFilePropertiesDependencies(
	private val filePropertiesRatePolicy: RateLimitingExecutionPolicy,
	inner: LibraryConnectionDependents,
) : LibraryConnectionDependents by inner {
	override val freshLibraryFileProperties by lazy {
		DelegatingFilePropertiesProvider(
			inner.freshLibraryFileProperties,
			filePropertiesRatePolicy,
		)
	}

	override val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			urlKeyProvider,
			FilePropertyCache,
			freshLibraryFileProperties,
		)
	}
}
