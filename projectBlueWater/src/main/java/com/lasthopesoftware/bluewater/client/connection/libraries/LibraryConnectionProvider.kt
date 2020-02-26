package com.lasthopesoftware.bluewater.client.connection.libraries

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
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
					promisedConnectionProvidersCache[libraryId]
						?.then({ c ->
							when (c) {
								null -> {
									proxy(promiseUpdatedCachedConnection(libraryId))
								}
								else -> {
									connectionTester.promiseIsConnectionPossible(c)
										.then({ result ->
											if (result) resolve(c)
											else proxy(promiseUpdatedCachedConnection(libraryId))
										}, {
											proxy(promiseUpdatedCachedConnection(libraryId))
										})
								}
							}
						}, {
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
					promisedConnectionProvidersCache[libraryId]?.then({
						if (it != null) resolve(it)
						else proxy(promiseUpdatedCachedConnection(libraryId))
					}, {
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
				promiseBuiltConnection(libraryId)
					.updates(OneParameterAction { reportProgress(it) })
					.then({ c ->
						if (c != null) cachedConnectionProviders[libraryId] = c
						resolve(c)
					}, { reject(it) })
			}
		}
	}

	private fun promiseBuiltConnection(selectedLibraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		return object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
			init {
				reportProgress(BuildingConnectionStatus.GettingLibrary)
				libraryProvider
					.getLibrary(selectedLibraryId)
					.eventually({ library ->
						when (library?.accessCode?.isEmpty()) {
							false -> {
								reportProgress(BuildingConnectionStatus.BuildingConnection)

								liveUrlProvider.promiseLiveUrl(library)
							}
							else -> {
								reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
								resolve(null)
								empty()
							}
						}
					}, {
						reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
						reject(it)
						empty()
					})
					.then({
						if (it != null) {
							reportProgress(BuildingConnectionStatus.BuildingConnectionComplete)
							resolve(ConnectionProvider(it, okHttpFactory))
						} else {
							reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
							resolve(null)
						}
					}, {
						reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
						reject(it)
					})
			}
		}
	}

	companion object Instance {
		private const val buildConnectionTimeoutTime = 10000

		private val libraryConnectionProviderReference = AtomicReference<LibraryConnectionProvider>()

		fun get(context: Context): LibraryConnectionProvider {
			val applicationContext = context.applicationContext

			val client = OkHttpClient.Builder()
				.connectTimeout(buildConnectionTimeoutTime.toLong(), TimeUnit.MILLISECONDS)
				.build()
			val serverLookup = ServerLookup(ServerInfoXmlRequest(LibraryRepository(applicationContext), client))
			val connectionTester = ConnectionTester()
			val urlScanner = UrlScanner(Base64Encoder(), connectionTester, serverLookup, OkHttpFactory.getInstance())

			val connectionProvider = libraryConnectionProviderReference.get()
				?: LibraryConnectionProvider(
					LibraryRepository(applicationContext),
					LiveUrlProvider(
						ActiveNetworkFinder(applicationContext),
						urlScanner),
					ConnectionTester(),
					OkHttpFactory.getInstance())

			libraryConnectionProviderReference.compareAndSet(null, connectionProvider)

			return connectionProvider
		}
	}
}
