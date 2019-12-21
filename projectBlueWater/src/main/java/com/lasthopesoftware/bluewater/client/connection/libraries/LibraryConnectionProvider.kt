package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.library.items.Item
import com.lasthopesoftware.bluewater.client.library.repository.Library
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.library.views.access.ProvideLibraryViewsUsingConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LibraryConnectionProvider(private val libraryProvider: ILibraryProvider, private val libraryStorage: ILibraryStorage, private val liveUrlProvider: ProvideLiveUrl, private val libraryViewsProvider: ProvideLibraryViewsUsingConnection) : ProvideLibraryConnections {

	private val cachedConnectionProviders = ConcurrentHashMap<Int, IConnectionProvider>()
	private val promisedConnectionProvidersCache = HashMap<Int, Promise<IConnectionProvider>>()

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	private fun promiseUpdatedCachedConnection(libraryId: Int): Promise<IConnectionProvider?>? {
		return promiseBuiltSessionConnection(libraryId)
			.then { c: IConnectionProvider? ->
				if (c != null) cachedConnectionProviders[libraryId] = c
				c
			}
	}

	private fun promiseBuiltSessionConnection(selectedLibraryId: Int): Promise<IConnectionProvider?> {
		doStateChange(BuildingSessionConnectionStatus.GettingLibrary)
		return libraryProvider
			.getLibrary(selectedLibraryId)
			.eventually({ library: Library? ->
				if (library == null || library.accessCode == null || library.accessCode.isEmpty()) {
					doStateChange(BuildingSessionConnectionStatus.GettingLibraryFailed)
					return Promise.empty<IConnectionProvider>()
				}
				doStateChange(BuildingSessionConnectionStatus.BuildingConnection)
				liveUrlProvider
					.promiseLiveUrl(library)
					.eventually({ urlProvider: IUrlProvider? ->
						if (urlProvider == null) {
							doStateChange(BuildingSessionConnectionStatus.BuildingConnectionFailed)
							return Promise.empty<IConnectionProvider>()
						}
						val localConnectionProvider: IConnectionProvider = ConnectionProvider(urlProvider, okHttpFactory)
						if (library.selectedView >= 0) {
							doStateChange(BuildingSessionConnectionStatus.BuildingSessionComplete)
							return Promise(localConnectionProvider)
						}
						doStateChange(BuildingSessionConnectionStatus.GettingView)
						libraryViewsProvider
							.promiseLibraryViewsFromConnection(localConnectionProvider)
							.eventually({ libraryViews: List<Item>? ->
								if (libraryViews == null || libraryViews.size == 0) {
									doStateChange(BuildingSessionConnectionStatus.GettingViewFailed)
									return Promise.empty<IConnectionProvider>()
								}
								val selectedView = libraryViews[0].key
								library.selectedView = selectedView
								library.selectedViewType = Library.ViewType.StandardServerView
								libraryStorage
									.saveLibrary(library)
									.then(ImmediateResponse<Library, IConnectionProvider> { savedLibrary: Library? ->
										doStateChange(BuildingSessionConnectionStatus.BuildingSessionComplete)
										localConnectionProvider
									})
							}, PromisedResponse<Throwable, IConnectionProvider?> { e: Throwable? ->
								doStateChange(BuildingSessionConnectionStatus.GettingViewFailed)
								Promise(e)
							})
					}, PromisedResponse<Throwable, IConnectionProvider?> { e: Throwable? ->
						doStateChange(BuildingSessionConnectionStatus.BuildingConnectionFailed)
						Promise(e)
					})
			}, PromisedResponse<Throwable, IConnectionProvider?> { e: Throwable? ->
				doStateChange(BuildingSessionConnectionStatus.GettingLibraryFailed)
				Promise(e)
			})
	}
}
