package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.connection.ServerConnection

class FakeHttpConnectionProvider(private val client: HttpPromiseClient) : ProvideHttpPromiseClients {
	override fun getServerClient(serverConnection: ServerConnection): HttpPromiseClient = client

	override fun getClient(): HttpPromiseClient = client
}
