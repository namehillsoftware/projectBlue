package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.library.items.Item
import com.lasthopesoftware.bluewater.client.library.repository.Library
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.library.views.access.ProvideLibraryViewsUsingConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.vedsoft.futures.runnables.OneParameterAction
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LibraryConnectionProvider(
	private val libraryProvider: ILibraryProvider,
	private val libraryStorage: ILibraryStorage,
	private val liveUrlProvider: ProvideLiveUrl,
	private val libraryViewsProvider: ProvideLibraryViewsUsingConnection,
	private val okHttpFactory: OkHttpFactory) : ProvideLibraryConnections {

	private val cachedConnectionProviders = ConcurrentHashMap<LibraryId, IConnectionProvider>()
	private val promisedConnectionProvidersCache = HashMap<LibraryId, ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>>()
	private val buildingConnectionPromiseSync = Any()

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		val cachedConnectionProvider = cachedConnectionProviders[libraryId]
		if (cachedConnectionProvider != null) return ProgressingPromise(cachedConnectionProvider)

		synchronized(buildingConnectionPromiseSync) {
			val cachedConnectionProvider = cachedConnectionProviders[libraryId]
			if (cachedConnectionProvider != null) return ProgressingPromise(cachedConnectionProvider)

			if (!promisedConnectionProvidersCache.containsKey(libraryId))
				promisedConnectionProvidersCache[libraryId] = ProgressingPromise(null as IConnectionProvider?)

			val cachedPromisedProvider = promisedConnectionProvidersCache[libraryId]
			val nextPromisedConnectionProvider = object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
				init {
					cachedPromisedProvider!!.then(
						{
							if (it != null) {
								resolve(it)
								return@then
							}

							promiseUpdatedCachedConnection(libraryId)
								.updates(OneParameterAction { reportProgress(it) })
								.then({resolve(it)}, {reject(it)})
						}
					) {
						promiseUpdatedCachedConnection(libraryId)
							.updates(OneParameterAction { reportProgress(it) })
							.then({resolve(it)}, {reject(it)})
					}
				}
			}
			promisedConnectionProvidersCache[libraryId] = nextPromisedConnectionProvider
			return nextPromisedConnectionProvider
		}
	}

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	private fun promiseUpdatedCachedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		return object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
			init {
				promiseBuiltSessionConnection(libraryId)
					.updates(OneParameterAction { reportProgress(it) })
					.then({ c: IConnectionProvider? ->
						if (c != null) cachedConnectionProviders[libraryId] = c
						resolve(c)
					}, { e: Throwable? -> reject(e) })
			}
		}
	}

	private fun promiseBuiltSessionConnection(selectedLibraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		return object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
			init {
				reportProgress(BuildingConnectionStatus.GettingLibrary)
				libraryProvider
					.getLibrary(selectedLibraryId.id)
					.then({ library: Library? ->
						if (library?.accessCode?.isEmpty() == true) {
							reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
							resolve(null)
							return@then
						}

						reportProgress(BuildingConnectionStatus.BuildingConnection)

						liveUrlProvider
							.promiseLiveUrl(library)
							.then({ urlProvider: IUrlProvider? ->
								if (urlProvider == null) {
									reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
									resolve(null)
									return@then
								}

								val localConnectionProvider: IConnectionProvider = ConnectionProvider(urlProvider, okHttpFactory)
								if (library!!.selectedView >= 0) {
									reportProgress(BuildingConnectionStatus.BuildingSessionComplete)
									resolve(localConnectionProvider)
									return@then
								}

								reportProgress(BuildingConnectionStatus.GettingView)

								libraryViewsProvider
									.promiseLibraryViewsFromConnection(localConnectionProvider)
									.then({ libraryViews: List<Item>? ->
										if (libraryViews == null || libraryViews.isEmpty()) {
											reportProgress(BuildingConnectionStatus.GettingViewFailed)
											resolve(null)
											return@then
										}

										val selectedView = libraryViews[0].key
										library.selectedView = selectedView
										library.selectedViewType = Library.ViewType.StandardServerView
										libraryStorage
											.saveLibrary(library)
											.then { savedLibrary: Library? ->
												reportProgress(BuildingConnectionStatus.BuildingSessionComplete)
												resolve(localConnectionProvider)
											}
									}, {
										reportProgress(BuildingConnectionStatus.GettingViewFailed)
										reject(it)
									})
							}, {
								reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
								reject(it)
							})
					}, {
						reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
						reject(it)
					})
			}
		}
	}
}
