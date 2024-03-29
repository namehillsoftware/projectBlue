package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.connection.waking.WakeLibraryServer
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class LibraryConnectionProvider(
	private val validateConnectionSettings: ValidateConnectionSettings,
	private val lookupConnectionSettings: LookupConnectionSettings,
	private val wakeAlarm: WakeLibraryServer,
	private val liveUrlProvider: ProvideLiveUrl,
	private val okHttpFactory: OkHttpFactory
) : ProvideLibraryConnections {

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> =
		object : ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>() {
			private val cancellationProxy = CancellationProxy()

			init {
				awaitCancellation(cancellationProxy)
				fulfillPromise()
			}

			private fun fulfillPromise() {
				reportProgress(BuildingConnectionStatus.GettingLibrary)
				lookupConnectionSettings
					.lookupConnectionSettings(libraryId)
					.eventually({ settings ->
						when {
							settings == null || !validateConnectionSettings.isValid(settings) -> {
								reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
								resolve(null)
								empty()
							}
							settings.isWakeOnLanEnabled -> wakeAndBuildConnection()
							else -> buildConnection()
						}
					}, {
						reportProgress(BuildingConnectionStatus.GettingLibraryFailed)
						reject(it)
						empty()
					})
					.then({ it ->
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
				if (cancellationProxy.isCancelled) return empty()

				reportProgress(BuildingConnectionStatus.SendingWakeSignal)
				return wakeAlarm
					.awakeLibraryServer(libraryId)
					.also(cancellationProxy::doCancel)
					.eventually { buildConnection() }
			}

			private fun buildConnection(): Promise<IUrlProvider?> {
				if (cancellationProxy.isCancelled) return empty()

				reportProgress(BuildingConnectionStatus.BuildingConnection)

				return liveUrlProvider.promiseLiveUrl(libraryId).also(cancellationProxy::doCancel)
			}
		}
}
