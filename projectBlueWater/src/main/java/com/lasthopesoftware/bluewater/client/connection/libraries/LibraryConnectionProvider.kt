package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.vedsoft.futures.runnables.OneParameterAction
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
			if (!promisedConnectionProvidersCache.containsKey(libraryId)) promisedConnectionProvidersCache[libraryId] = ProgressingPromise(null as IConnectionProvider?)

			val promisedTestConnectionProvider = object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>()
			{
				init
				{
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

			if (!promisedConnectionProvidersCache.containsKey(libraryId))
				promisedConnectionProvidersCache[libraryId] = ProgressingPromise(null as IConnectionProvider?)

			val cachedPromisedProvider = promisedConnectionProvidersCache[libraryId]
			val nextPromisedConnectionProvider = object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>()
			{
				init
				{
					cachedPromisedProvider!!.then(
					{
						if (it != null) resolve(it)
						else proxy(promiseUpdatedCachedConnection(libraryId))
					},
					{
						proxy(promiseUpdatedCachedConnection(libraryId))
					})
				}
			}
			promisedConnectionProvidersCache[libraryId] = nextPromisedConnectionProvider
			return nextPromisedConnectionProvider
		}
	}

	private fun promiseUpdatedCachedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		return object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>() {
			init
			{
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
		return object : ProgressingPromise<BuildingConnectionStatus, IConnectionProvider>()
		{
			init
			{
				reportProgress(BuildingConnectionStatus.GettingLibrary)
				libraryProvider
					.getLibrary(selectedLibraryId.id)
					.then(
					{ library ->
						when(library?.accessCode?.isEmpty())
						{
							true ->
							{
								reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
								resolve(null)
							}
							else ->
							{
								reportProgress(BuildingConnectionStatus.BuildingConnection)

								liveUrlProvider
									.promiseLiveUrl(library)
									.then(
									onFulfilled@{ urlProvider ->
										if (urlProvider == null)
										{
											reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
											resolve(null)
											return@onFulfilled
										}

										val localConnectionProvider = ConnectionProvider(urlProvider, okHttpFactory)
										reportProgress(BuildingConnectionStatus.BuildingSessionComplete)
										resolve(localConnectionProvider)
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
}
