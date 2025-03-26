package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails

interface ProvideHttpPromiseClients {
	fun getServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails): HttpPromiseClient
	fun getServerClient(subsonicConnectionDetails: SubsonicConnectionDetails): HttpPromiseClient

	fun getClient(): HttpPromiseClient
}
