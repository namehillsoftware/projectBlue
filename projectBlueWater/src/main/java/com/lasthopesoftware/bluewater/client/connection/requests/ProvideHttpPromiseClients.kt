package com.lasthopesoftware.bluewater.client.connection.requests

interface ProvideHttpPromiseClients {
	fun getClient(): HttpPromiseClient
}

interface ProvideHttpPromiseServerClients<TConnectionDetails> {
	fun getServerClient(connectionDetails: TConnectionDetails): HttpPromiseClient
	fun getStreamingServerClient(connectionDetails: TConnectionDetails): HttpPromiseClient
}

