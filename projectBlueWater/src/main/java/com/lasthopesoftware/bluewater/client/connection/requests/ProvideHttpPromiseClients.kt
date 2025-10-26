package com.lasthopesoftware.bluewater.client.connection.requests

import com.namehillsoftware.handoff.promises.Promise

interface ProvideHttpPromiseClients {
	fun promiseClient(): Promise<HttpPromiseClient>
}

interface ProvideHttpPromiseServerClients<TConnectionDetails> {
	fun promiseServerClient(connectionDetails: TConnectionDetails): Promise<HttpPromiseClient>
	fun promiseStreamingServerClient(connectionDetails: TConnectionDetails): Promise<HttpPromiseClient>
}

