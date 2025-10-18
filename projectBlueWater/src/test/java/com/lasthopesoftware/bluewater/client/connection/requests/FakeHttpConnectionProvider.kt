package com.lasthopesoftware.bluewater.client.connection.requests

class FakeHttpConnectionProvider<TConnectionDetails>(private val client: HttpPromiseClient) : ProvideHttpPromiseClients, ProvideHttpPromiseServerClients<TConnectionDetails> {
	override fun getServerClient(connectionDetails: TConnectionDetails): HttpPromiseClient = client
	override fun getStreamingServerClient(connectionDetails: TConnectionDetails): HttpPromiseClient = client

	override fun getClient(): HttpPromiseClient = client
}
