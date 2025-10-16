package com.lasthopesoftware.bluewater.client.playback.exoplayer

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpServerClients
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseServerClients
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.namehillsoftware.handoff.promises.Promise

class ServerHttpDataSourceProvider<TConnectionDetails>(
	private val httpPromiseClients: ProvideHttpPromiseServerClients<TConnectionDetails>,
	private val okHttpClients: ProvideOkHttpServerClients<TConnectionDetails>,
	private val applicationFeatureConfiguration: HoldApplicationFeatureConfiguration,
): ProvideServerHttpDataSource<TConnectionDetails> {
	override fun promiseDataSourceFactory(connectionDetails: TConnectionDetails): Promise<DataSource.Factory> =
		applicationFeatureConfiguration
			.promiseFeatureConfiguration()
			.then { featureConfiguration ->
				when (featureConfiguration.httpDataSourceType ?: HttpDataSourceType.OkHttp) {
					HttpDataSourceType.HttpPromiseClient -> HttpPromiseClientDataSource.Factory(
						httpPromiseClients.getStreamingServerClient(connectionDetails)
					)
					HttpDataSourceType.OkHttp -> OkHttpDataSource.Factory(
						okHttpClients.getStreamingOkHttpClient(connectionDetails)
					)
				}
			}
}
