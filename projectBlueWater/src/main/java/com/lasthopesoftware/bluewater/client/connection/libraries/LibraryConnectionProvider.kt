package com.lasthopesoftware.bluewater.client.connection.libraries

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.client.connection.waking.WakeLibraryServer
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.namehillsoftware.handoff.promises.Promise
import com.vedsoft.futures.runnables.OneParameterAction
import okhttp3.OkHttpClient
import org.joda.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class LibraryConnectionProvider(
	private val libraryProvider: ILibraryProvider,
	private val wakeAlarm: WakeLibraryServer,
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
								if (library.isWakeOnLanEnabled) wakeAndBuildConnection(library)
								else buildConnection(library)
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

			private fun wakeAndBuildConnection(library: Library): Promise<IUrlProvider> {
				reportProgress(BuildingConnectionStatus.SendingWakeSignal)
				return wakeAlarm.awakeLibraryServer(selectedLibraryId)
					.eventually { buildConnection(library) }
			}

			private fun buildConnection(library: Library): Promise<IUrlProvider> {
				reportProgress(BuildingConnectionStatus.BuildingConnection)

				return liveUrlProvider.promiseLiveUrl(library)
			}
		}
	}

	companion object Instance {
		private const val buildConnectionTimeoutTime = 10000

		private val libraryConnectionProviderReference = AtomicReference<LibraryConnectionProvider>()

		private fun newServerLookup(context: Context): ServerLookup {
			val client = OkHttpClient.Builder()
				.connectTimeout(buildConnectionTimeoutTime.toLong(), TimeUnit.MILLISECONDS)
				.build()
			return ServerLookup(ServerInfoXmlRequest(LibraryRepository(context), client))
		}

		private fun newUrlScanner(context: Context): UrlScanner {
			return UrlScanner(
				Base64Encoder(),
				ConnectionTester(),
				newServerLookup(context),
				OkHttpFactory.getInstance())
		}

		fun get(context: Context): LibraryConnectionProvider {
			val applicationContext = context.applicationContext

			val connectionProvider = libraryConnectionProviderReference.get()
				?: LibraryConnectionProvider(
					LibraryRepository(applicationContext),
					ServerAlarm(
						newServerLookup(context),
						ServerWakeSignal(PacketSender()),
						AlarmConfiguration(3, Duration.standardSeconds(5))),
					LiveUrlProvider(
						ActiveNetworkFinder(applicationContext),
						newUrlScanner(context)),
					ConnectionTester(),
					OkHttpFactory.getInstance())

			libraryConnectionProviderReference.compareAndSet(null, connectionProvider)

			return connectionProvider
		}
	}
}
