package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.connection.ServerConnection

interface ProvideHttpPromiseClients {
	fun getServerClient(serverConnection: ServerConnection): HttpPromiseClient

	fun getClient(): HttpPromiseClient
}
