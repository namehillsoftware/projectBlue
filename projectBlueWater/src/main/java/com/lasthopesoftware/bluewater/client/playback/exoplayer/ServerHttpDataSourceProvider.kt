package com.lasthopesoftware.bluewater.client.playback.exoplayer

import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseServerClients
import com.namehillsoftware.handoff.promises.Promise

class ServerHttpDataSourceProvider<TConnectionDetails>(
	private val httpPromiseClients: ProvideHttpPromiseServerClients<TConnectionDetails>,
): ProvideServerHttpDataSource<TConnectionDetails> {
	override fun promiseDataSourceFactory(connectionDetails: TConnectionDetails): Promise<HttpDataSource.Factory> =
		httpPromiseClients
			.promiseStreamingServerClient(connectionDetails)
			.then(HttpPromiseClientDataSource::Factory)
}
