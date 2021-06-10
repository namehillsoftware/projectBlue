package com.lasthopesoftware.bluewater.client.connection.libraries

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
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
import okhttp3.OkHttpClient
import org.joda.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class LibraryConnectionProvider(
	private val libraryProvider: ILibraryProvider,
	private val validateConnectionSettings: ValidateConnectionSettings,
	private val lookupConnectionSettings: LookupConnectionSettings,
	private val wakeAlarm: WakeLibraryServer,
	private val liveUrlProvider: ProvideLiveUrl,
	private val connectionTester: TestConnections,
	private val okHttpFactory: OkHttpFactory) : ProvideLibraryConnections {

	private val cachedConnectionProviders = ConcurrentHashMap<LibraryId, IConnectionProvider>()
	private val promisedConnectionProvidersCache = HashMap<LibraryId, ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>>()
	private val buildingConnectionPromiseSync = Any()

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> {
		synchronized(buildingConnectionPromiseSync) {
			val promisedTestConnectionProvider = object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>() {
				init {
					promisedConnectionProvidersCache[libraryId]
						?.then({ c ->
							c?.let {
								connectionTester.promiseIsConnectionPossible(it)
									.then({ isPossible ->
										if (isPossible) resolve(it)
										else proxy(promiseUpdatedCachedConnection(libraryId))
									}, {
										proxy(promiseUpdatedCachedConnection(libraryId))
									})
							} ?: proxy(promiseUpdatedCachedConnection(libraryId))
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

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		cachedConnectionProviders[libraryId]?.let { ProgressingPromise(it) } ?:
			synchronized(buildingConnectionPromiseSync) {
				cachedConnectionProviders[libraryId]?.let { ProgressingPromise(it) } ?:
					object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>() {
						init {
							promisedConnectionProvidersCache[libraryId]?.then({
								it?.apply(::resolve) ?: proxy(promiseUpdatedCachedConnection(libraryId))
							}, {
								proxy(promiseUpdatedCachedConnection(libraryId))
							})
							?: proxy(promiseUpdatedCachedConnection(libraryId))
						}
					}.also { promisedConnectionProvidersCache[libraryId] = it }
			}

	override fun isConnectionActive(libraryId: LibraryId): Boolean {
		return cachedConnectionProviders[libraryId] != null
	}

	private fun promiseUpdatedCachedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> {
		return object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>() {
			init {
				promiseBuiltConnection(libraryId)
					.updates { reportProgress(it) }
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
				lookupConnectionSettings
					.lookupConnectionSettings(selectedLibraryId)
					.eventually({ connectionSettings ->
						connectionSettings?.let {
							if (validateConnectionSettings.isValid(it)) {
								if (it.isWakeOnLanEnabled) wakeAndBuildConnection()
								else buildConnection()
							} else {
								reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
								resolve(null)
								empty()
							}
						} ?: empty()
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

			private fun wakeAndBuildConnection(): Promise<IUrlProvider?> {
				reportProgress(BuildingConnectionStatus.SendingWakeSignal)
				return wakeAlarm
					.awakeLibraryServer(selectedLibraryId)
					.eventually { buildConnection() }
			}

			private fun buildConnection(): Promise<IUrlProvider?> {
				reportProgress(BuildingConnectionStatus.BuildingConnection)

				return liveUrlProvider.promiseLiveUrl(selectedLibraryId)
			}
		}
	}

	companion object Instance {
		private val syncObject = Object()
		private const val buildConnectionTimeoutTime = 10000

		private var libraryConnectionProvider: LibraryConnectionProvider? = null

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
				ConnectionSettingsLookup(LibraryRepository(context)),
				OkHttpFactory.getInstance())
		}

		fun get(context: Context): LibraryConnectionProvider {
			val applicationContext = context.applicationContext

			return libraryConnectionProvider
				?: synchronized(syncObject) {
					libraryConnectionProvider ?: LibraryConnectionProvider(
						LibraryRepository(applicationContext),
						ConnectionSettingsValidation,
						ConnectionSettingsLookup(LibraryRepository(applicationContext)),
						ServerAlarm(
							newServerLookup(context),
							ServerWakeSignal(PacketSender()),
							AlarmConfiguration(3, Duration.standardSeconds(5))),
						LiveUrlProvider(
							ActiveNetworkFinder(applicationContext),
							newUrlScanner(context)),
						ConnectionTester(),
						OkHttpFactory.getInstance())
						.also { libraryConnectionProvider = it }
				}

		}
	}
}
