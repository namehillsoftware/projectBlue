package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface ProvideHttpPromiseClients {
	fun getServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails): HttpPromiseClient
	fun getServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails, clientOptions: HttpPromiseClientOptions): HttpPromiseClient
	fun getServerClient(subsonicConnectionDetails: SubsonicConnectionDetails): HttpPromiseClient

	fun getClient(): HttpPromiseClient
	fun getServerClient(
		subsonicConnectionDetails: SubsonicConnectionDetails,
		clientOptions: HttpPromiseClientOptions
	): HttpPromiseClient
}

data class HttpPromiseClientOptions(
	val readTimeout: Duration = 10.seconds,
	val retryOnConnectionFailure: Boolean = true,
)
