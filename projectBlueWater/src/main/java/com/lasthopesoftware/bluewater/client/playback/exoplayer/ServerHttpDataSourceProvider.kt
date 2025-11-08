package com.lasthopesoftware.bluewater.client.playback.exoplayer

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.lasthopesoftware.bluewater.client.connection.http.ProvideOkHttpServerClients
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseServerClients
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class ServerHttpDataSourceProvider<TConnectionDetails>(
	private val httpPromiseClients: ProvideHttpPromiseServerClients<TConnectionDetails>,
	private val okHttpClients: ProvideOkHttpServerClients<TConnectionDetails>,
	private val applicationFeatureConfiguration: HoldApplicationFeatureConfiguration,
): ProvideServerHttpDataSource<TConnectionDetails> {
	override fun promiseDataSourceFactory(connectionDetails: TConnectionDetails): Promise<DataSource.Factory> =
		applicationFeatureConfiguration
			.promiseFeatureConfiguration()
			.eventually { featureConfiguration ->
				when (featureConfiguration.httpDataSourceType ?: HttpDataSourceType.OkHttp) {
					HttpDataSourceType.HttpPromiseClient -> httpPromiseClients
						.promiseStreamingServerClient(connectionDetails)
						.then(HttpPromiseClientDataSource::Factory)
					HttpDataSourceType.OkHttp -> OkHttpDataSource.Factory(
						okHttpClients.getStreamingOkHttpClient(connectionDetails)
					).toPromise()
				}
			}
}
