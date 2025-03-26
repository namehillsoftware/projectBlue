package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails

class FakeHttpConnectionProvider(private val client: HttpPromiseClient) : ProvideHttpPromiseClients {
	override fun getServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails): HttpPromiseClient = client
	override fun getServerClient(subsonicConnectionDetails: SubsonicConnectionDetails): HttpPromiseClient = client
	override fun getClient(): HttpPromiseClient = client
}
