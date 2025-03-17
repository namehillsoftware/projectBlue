package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.WakeLibraryServer
import com.lasthopesoftware.promises.PromiseDelay
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import java.util.concurrent.CancellationException

class LibraryConnectionProvider(
	private val validateConnectionSettings: ValidateConnectionSettings,
	private val lookupConnectionSettings: LookupConnectionSettings,
	private val wakeAlarm: WakeLibraryServer,
	private val liveUrlProvider: ProvideLiveServerConnection,
	private val alarmConfiguration: AlarmConfiguration,
) : ProvideLibraryConnections {

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> =
		object : ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>() {
			private val cancellationProxy = CancellationProxy()

			@Volatile
			private var wakeAttempts = 0

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
					.then({
						if (it != null) {
							reportProgress(BuildingConnectionStatus.BuildingConnectionComplete)
							resolve(it)
						} else {
							reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
							resolve(null)
						}
					}, { e ->
						reportProgress(BuildingConnectionStatus.BuildingConnectionFailed)
						if (e is CancellationException) resolve(null)
						else reject(e)
					})
			}

			private fun wakeAndBuildConnection(): Promise<LiveServerConnection?> {
				if (cancellationProxy.isCancelled) return empty()

				return buildConnection()
					.eventually({
						when {
							it != null -> it.toPromise()
							wakeAttempts < alarmConfiguration.timesToWake -> attemptToWake()
							else -> empty()
						}
					}, { e ->
						if (wakeAttempts < alarmConfiguration.timesToWake) attemptToWake() else Promise(e)
					})
			}

			private fun attemptToWake(): Promise<LiveServerConnection?> {
				if (cancellationProxy.isCancelled) return empty()

				++wakeAttempts

				reportProgress(BuildingConnectionStatus.SendingWakeSignal)
				return wakeAlarm
					.awakeLibraryServer(libraryId)
					.also(cancellationProxy::doCancel)
					.eventually {
						PromiseDelay
							.delay<Unit>(alarmConfiguration.durationBetweenWaking)
							.also(cancellationProxy::doCancel)
					}
					.eventually {
						wakeAndBuildConnection()
					}
			}

			private fun buildConnection(): Promise<LiveServerConnection?> {
				if (cancellationProxy.isCancelled) return empty()

				reportProgress(BuildingConnectionStatus.BuildingConnection)

				return liveUrlProvider.promiseLiveServerConnection(libraryId).also(cancellationProxy::doCancel)
			}
		}
}
