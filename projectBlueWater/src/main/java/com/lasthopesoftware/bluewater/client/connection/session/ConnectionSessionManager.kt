package com.lasthopesoftware.bluewater.client.connection.session

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsValidation
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import okhttp3.OkHttpClient
import org.joda.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ConnectionSessionManager(
	private val connectionTester: TestConnections,
	private val libraryConnections: ProvideLibraryConnections
) : ManageConnectionSessions, ProvideLibraryConnections {

	private val cachedConnectionProviders = ConcurrentHashMap<LibraryId, IConnectionProvider>()
	private val promisedConnectionProvidersCache = HashMap<LibraryId, ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>>()
	private val buildingConnectionPromiseSync = Any()

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		synchronized(buildingConnectionPromiseSync) {
			val promisedTestConnectionProvider = object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
				init {
					promisedConnectionProvidersCache[libraryId]
						?.then({ c ->
							c?.let {
								connectionTester.promiseIsConnectionPossible(it)
									.then({ isPossible ->
										if (isPossible) resolve(it)
										else updateCachedConnection()
									}, {
										updateCachedConnection()
									})
							} ?: updateCachedConnection()
						}, {
							updateCachedConnection()
						})
						?: updateCachedConnection()
				}

				private fun updateCachedConnection() = proxy(promiseUpdatedCachedConnection(libraryId))
			}
			promisedConnectionProvidersCache[libraryId] = promisedTestConnectionProvider
			promisedTestConnectionProvider
		}

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		cachedConnectionProviders[libraryId]?.let { ProgressingPromise(it) } ?:
			synchronized(buildingConnectionPromiseSync) {
				cachedConnectionProviders[libraryId]?.let { ProgressingPromise(it) } ?:
					promisedConnectionProvidersCache[libraryId]?.apply {
						eventuallyExcuse {
							promiseUpdatedCachedConnection(libraryId).also { promisedConnectionProvidersCache[libraryId] = it }
						}
					} ?: promiseUpdatedCachedConnection(libraryId).also { promisedConnectionProvidersCache[libraryId] = it }
			}

	override fun removeConnection(libraryId: LibraryId) {
		synchronized(buildingConnectionPromiseSync) {
			promisedConnectionProvidersCache[libraryId]?.apply {
				cancel()
				must { synchronized(buildingConnectionPromiseSync) {
					cachedConnectionProviders.remove(libraryId)
				} }
			} ?: cachedConnectionProviders.remove(libraryId)
		}
	}

	override fun isConnectionActive(libraryId: LibraryId): Boolean =
		cachedConnectionProviders[libraryId] != null

	private fun promiseUpdatedCachedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>() {
			init {
				val cancellationProxy = CancellationProxy()
				respondToCancellation(cancellationProxy)

				val promisedLibraryConnection = libraryConnections.promiseLibraryConnection(libraryId)
				cancellationProxy.doCancel(promisedLibraryConnection)

				promisedLibraryConnection
					.updates(::reportProgress)
					.then({ c ->
						if (c != null) cachedConnectionProviders[libraryId] = c
						resolve(c)
					}, { reject(it) })
			}
		}

	companion object Instance {
		private val syncObject = Object()
		private const val buildConnectionTimeoutTime = 10000

		private var connectionSessionManager: ConnectionSessionManager? = null

		private fun newServerLookup(context: Context): ServerLookup {
			val client = OkHttpClient.Builder()
				.connectTimeout(buildConnectionTimeoutTime.toLong(), TimeUnit.MILLISECONDS)
				.build()
			return ServerLookup(ServerInfoXmlRequest(LibraryRepository(context), client))
		}

		private fun newUrlScanner(context: Context): UrlScanner =
			UrlScanner(
				Base64Encoder(),
				ConnectionTester,
				newServerLookup(context),
				ConnectionSettingsLookup(LibraryRepository(context)),
				OkHttpFactory.getInstance())

		fun get(context: Context): ConnectionSessionManager {
			val applicationContext = context.applicationContext

			return connectionSessionManager
				?: synchronized(syncObject) {
					connectionSessionManager ?: ConnectionSessionManager(
						ConnectionTester,
						LibraryConnectionProvider(
							ConnectionSettingsValidation,
							ConnectionSettingsLookup(LibraryRepository(applicationContext)),
							ServerAlarm(
								newServerLookup(context),
								ServerWakeSignal(PacketSender()),
								AlarmConfiguration(3, Duration.standardSeconds(5))),
							LiveUrlProvider(
								ActiveNetworkFinder(applicationContext),
								newUrlScanner(context)
							),
							OkHttpFactory.getInstance())
					).also { connectionSessionManager = it }
				}
		}
	}
}
