package com.lasthopesoftware.bluewater.client.connection.libraries

import android.content.Context
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.vedsoft.futures.runnables.OneParameterAction
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class LibraryConnectionProvider(
	private val libraryProvider: ILibraryProvider,
	private val liveUrlProvider: ProvideLiveUrl,
	private val connectionTester: TestConnections,
	private val okHttpFactory: OkHttpFactory) : ProvideLibraryConnections {

	private val cachedConnectionProviders = ConcurrentHashMap<LibraryId, IConnectionProvider>()
	private val promisedConnectionProvidersCache = HashMap<LibraryId, ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>>()
	private val buildingConnectionPromiseSync = Any()

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		synchronized(buildingConnectionPromiseSync) {
			val promisedTestConnectionProvider = object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
				init {
					promisedConnectionProvidersCache[libraryId]?.then(
						{ c ->
							if (c != null) connectionTester.promiseIsConnectionPossible(c)
								.then(
									{ result ->
										if (result) resolve(c)
										else proxy(promiseUpdatedCachedConnection(libraryId))
									},
									{
										proxy(promiseUpdatedCachedConnection(libraryId))
									})
							else proxy(promiseUpdatedCachedConnection(libraryId))
						},
						{
							proxy(promiseUpdatedCachedConnection(libraryId))
						})
						?: proxy(promiseUpdatedCachedConnection(libraryId))
				}
			}
			promisedConnectionProvidersCache[libraryId] = promisedTestConnectionProvider
			return promisedTestConnectionProvider
		}
	}

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		val cachedConnectionProvider = cachedConnectionProviders[libraryId]
		if (cachedConnectionProvider != null) return ProgressingPromise(cachedConnectionProvider)

		synchronized(buildingConnectionPromiseSync) {
			val cachedConnectionProvider = cachedConnectionProviders[libraryId]
			if (cachedConnectionProvider != null) return ProgressingPromise(cachedConnectionProvider)

			val nextPromisedConnectionProvider = object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
				init {
					promisedConnectionProvidersCache[libraryId]?.then(
						{
							if (it != null) resolve(it)
							else proxy(promiseUpdatedCachedConnection(libraryId))
						},
						{
							proxy(promiseUpdatedCachedConnection(libraryId))
						})
						?: proxy(promiseUpdatedCachedConnection(libraryId))
				}
			}
			promisedConnectionProvidersCache[libraryId] = nextPromisedConnectionProvider
			return nextPromisedConnectionProvider
		}
	}

	private fun promiseUpdatedCachedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		return object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
			init {
				promiseBuiltSessionConnection(libraryId)
					.updates(OneParameterAction { reportProgress(it) })
					.then(
						{ c ->
							if (c != null) cachedConnectionProviders[libraryId] = c
							resolve(c)
						},
						{ reject(it) })
			}
		}
	}

	private fun promiseBuiltSessionConnection(selectedLibraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		return object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
			init {
				reportProgress(BuildingConnectionStatus.GettingLibrary)
				libraryProvider
					.getLibrary(selectedLibraryId.id)
					.then(
						{ library ->
							when (library?.accessCode?.isEmpty()) {
								null, true -> {
									reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
									resolve(null)
								}
								else -> {
									reportProgress(BuildingConnectionStatus.BuildingConnection)

									liveUrlProvider
										.promiseLiveUrl(library)
										.then(
											{
												when (it) {
													null -> {
														reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
														resolve(null)
													}
													else -> {
														reportProgress(BuildingConnectionStatus.BuildingConnectionComplete)
														resolve(ConnectionProvider(it, okHttpFactory))
													}
												}
											},
											{
												reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
												reject(it)
											})
								}
							}
						},
						{
							reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
							reject(it)
						})
			}
		}
	}

	companion object Instance {
		private const val buildConnectionTimeoutTime = 10000

		private val libraryConnectionProviderReference = AtomicReference<LibraryConnectionProvider>()

		private val lazyUrlScanner = object : AbstractSynchronousLazy<BuildUrlProviders>() {
			override fun create(): BuildUrlProviders {
				val client = OkHttpClient.Builder()
					.connectTimeout(buildConnectionTimeoutTime.toLong(), TimeUnit.MILLISECONDS)
					.build()
				val serverLookup = ServerLookup(ServerInfoXmlRequest(client))
				val connectionTester = ConnectionTester()
				return UrlScanner(Base64Encoder(), connectionTester, serverLookup, OkHttpFactory.getInstance())
			}
		}

		fun get(context: Context): LibraryConnectionProvider {
			val connectionProvider = libraryConnectionProviderReference.get()
				?: LibraryConnectionProvider(
					LibraryRepository(context.applicationContext),
					LiveUrlProvider(
						ActiveNetworkFinder(context.applicationContext),
						lazyUrlScanner.getObject()),
					ConnectionTester(),
					OkHttpFactory.getInstance())

			libraryConnectionProviderReference.compareAndSet(null, connectionProvider)

			return connectionProvider
		}
	}
}
