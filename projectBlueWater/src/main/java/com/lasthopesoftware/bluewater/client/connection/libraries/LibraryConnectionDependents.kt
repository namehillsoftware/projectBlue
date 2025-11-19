package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.ApplicationDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.access.DelegatingLibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.RevisionCachedLibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.DelegatingFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FreshestRevisionFilePropertiesProvider
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
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.DelegatingLibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPollTimes
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPollingSessions
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.policies.caching.CachePromiseFunctions
import com.lasthopesoftware.policies.caching.CachingPolicyFactory
import com.lasthopesoftware.policies.caching.LruPromiseCache
import com.lasthopesoftware.policies.caching.TimedExpirationPromiseCache
import com.lasthopesoftware.policies.ratelimiting.RateLimitingExecutionPolicy
import com.lasthopesoftware.policies.retries.RecursivePromiseRetryHandler
import com.lasthopesoftware.policies.retries.RetryExecutionPolicy
import org.joda.time.Duration

interface LibraryConnectionDependents {
	val urlKeyProvider: UrlKeyProvider
	val revisionProvider: CheckRevisions
	val filePropertiesStorage: FilePropertyStorage
	val itemProvider: ProvideItems
	val libraryFilesProvider: ProvideLibraryFiles
	val playbackLibraryItems: PlaybackLibraryItems
	val pollForConnections: PollForLibraryConnections
	val libraryFilePropertiesProvider: CachedFilePropertiesProvider
	val freshLibraryFileProperties: ProvideFreshLibraryFileProperties
	val connectionAuthenticationChecker: ConnectionAuthenticationChecker
}

class LibraryConnectionRegistry(application: ApplicationDependencies) : LibraryConnectionDependents {
	companion object {
		private val revisionExpirationTime by lazy { Duration.standardSeconds(30) }
		private const val maxLibraryFiles = 10
	}

	private val guaranteedLibraryConnectionProvider by lazy { GuaranteedLibraryConnectionProvider(application.libraryConnectionProvider) }

	override val connectionAuthenticationChecker by lazy { ConnectionAuthenticationChecker(application.libraryConnectionProvider) }

	override val freshLibraryFileProperties: ProvideFreshLibraryFileProperties by lazy {
		FreshestRevisionFilePropertiesProvider(
			FilePropertiesProvider(guaranteedLibraryConnectionProvider),
			urlKeyProvider,
			revisionProvider,
			FilePropertyCache,
		)
	}

	override val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			urlKeyProvider,
			freshLibraryFileProperties,
			FilePropertyCache,
		)
	}

	override val urlKeyProvider by lazy { UrlKeyProvider(application.libraryConnectionProvider) }

	override val revisionProvider by lazy {
		DelegatingLibraryRevisionProvider(
			LibraryRevisionProvider(application.libraryConnectionProvider),
			object : CachingPolicyFactory() {
				override fun <Input : Any, Output> getCache(): CachePromiseFunctions<Input, Output> =
					TimedExpirationPromiseCache(revisionExpirationTime)
			}
		)
	}

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

	override val libraryFilesProvider by lazy {
		RevisionCachedLibraryFileProvider(
			LibraryFileProvider(application.libraryConnectionProvider),
			revisionProvider,
			object : CachingPolicyFactory() {
				override fun <Input : Any, Output> getCache(): CachePromiseFunctions<Input, Output> =
					LruPromiseCache(maxLibraryFiles)
			}
		)
	}

	override val playbackLibraryItems by lazy { ItemPlayback(libraryFilesProvider, application.playbackServiceController) }

	override val pollForConnections by lazy {
		LibraryConnectionPollingSessions(LibraryConnectionPoller(application.connectionSessions, ConnectionPollTimes))
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

	override val freshLibraryFileProperties by lazy {
		DelegatingFilePropertiesProvider(
			inner.freshLibraryFileProperties,
			connectionLostRetryPolicy,
		)
	}

	override val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			urlKeyProvider,
			freshLibraryFileProperties,
			FilePropertyCache,
		)
	}

	override val libraryFilesProvider by lazy {
		DelegatingLibraryFileProvider(
			inner.libraryFilesProvider,
			connectionLostRetryPolicy,
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
			freshLibraryFileProperties,
			FilePropertyCache,
		)
	}
}
