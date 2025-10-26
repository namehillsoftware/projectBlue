package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class FakeHttpConnectionProvider<TConnectionDetails>(private val client: HttpPromiseClient) : ProvideHttpPromiseClients, ProvideHttpPromiseServerClients<TConnectionDetails> {
	override fun promiseServerClient(connectionDetails: TConnectionDetails): Promise<HttpPromiseClient> = client.toPromise()
	override fun promiseStreamingServerClient(connectionDetails: TConnectionDetails): Promise<HttpPromiseClient> = client.toPromise()

	override fun promiseClient(): Promise<HttpPromiseClient> = client.toPromise()
}
