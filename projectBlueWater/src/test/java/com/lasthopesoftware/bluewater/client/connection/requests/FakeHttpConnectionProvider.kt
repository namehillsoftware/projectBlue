package com.lasthopesoftware.bluewater.client.connection.requests

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class FakeHttpConnectionProvider(private val client: HttpPromiseClient) : ProvideHttpPromiseClients {
	override fun promiseServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails): Promise<HttpPromiseClient> = client.toPromise()
	override fun promiseServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails, clientOptions: HttpPromiseClientOptions): Promise<HttpPromiseClient> = client.toPromise()

	override fun promiseServerClient(subsonicConnectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient> = client.toPromise()
	override fun promiseClient(): Promise<HttpPromiseClient> = client.toPromise()
	override fun promiseServerClient(subsonicConnectionDetails: SubsonicConnectionDetails, clientOptions: HttpPromiseClientOptions): Promise<HttpPromiseClient> = client.toPromise()
}
