package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.namehillsoftware.handoff.promises.Promise
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface ProvideHttpPromiseClients {
	fun promiseServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails): Promise<HttpPromiseClient>
	fun promiseServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails, clientOptions: HttpPromiseClientOptions): Promise<HttpPromiseClient>
	fun promiseServerClient(subsonicConnectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient>

	fun promiseClient(): Promise<HttpPromiseClient>
	fun promiseServerClient(
		subsonicConnectionDetails: SubsonicConnectionDetails,
		clientOptions: HttpPromiseClientOptions
	): Promise<HttpPromiseClient>
}

data class HttpPromiseClientOptions(
	val readTimeout: Duration = 10.seconds,
	val retryOnConnectionFailure: Boolean = true,
)
