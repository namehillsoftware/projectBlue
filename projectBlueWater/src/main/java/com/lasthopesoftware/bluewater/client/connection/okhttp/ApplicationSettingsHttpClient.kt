package com.lasthopesoftware.bluewater.client.connection.okhttp

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseServerClients
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.namehillsoftware.handoff.promises.Promise

class ApplicationSettingsHttpClient(
	private val applicationFeatureConfiguration: HoldApplicationFeatureConfiguration,
	private val okHttpFactory: OkHttpFactory,
	private val ktorFactory: KtorFactory,
) : ProvideHttpPromiseClients {
	override fun promiseClient(): Promise<HttpPromiseClient> =
		promiseFactory()
			.eventually { factory ->
				factory.promiseClient()
			}

	private fun promiseFactory() = applicationFeatureConfiguration
		.promiseFeatureConfiguration()
		.then { features ->
			val clientType = features?.httpClientType ?: HttpClientType.OkHttp
			when (clientType) {
				HttpClientType.OkHttp -> okHttpFactory
				HttpClientType.Ktor -> ktorFactory
			}
		}
}

class ApplicationSettingsSubsonicClient(
	private val applicationFeatureConfiguration: HoldApplicationFeatureConfiguration,
	private val okHttpClient: OkHttpFactory.SubsonicClient,
	private val ktorClient: KtorFactory.SubsonicClient,
) : ProvideHttpPromiseServerClients<SubsonicConnectionDetails> {
	override fun promiseServerClient(connectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient> =
		promiseFactory().eventually { factory -> factory.promiseServerClient(connectionDetails) }

	override fun promiseStreamingServerClient(connectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient> =
		promiseFactory().eventually { factory -> factory.promiseStreamingServerClient(connectionDetails) }

	private fun promiseFactory() = applicationFeatureConfiguration
		.promiseFeatureConfiguration()
		.then { features ->
			val clientType = features?.httpClientType ?: HttpClientType.OkHttp
			when (clientType) {
				HttpClientType.OkHttp -> okHttpClient
				HttpClientType.Ktor -> ktorClient
			}
		}
}

class ApplicationSettingsMediaCenterClient(
	private val applicationFeatureConfiguration: HoldApplicationFeatureConfiguration,
	private val okHttpClient: OkHttpFactory.MediaCenterClient,
	private val ktorClient: KtorFactory.MediaCenterClient,
) : ProvideHttpPromiseServerClients<MediaCenterConnectionDetails> {
	override fun promiseServerClient(connectionDetails: MediaCenterConnectionDetails): Promise<HttpPromiseClient> =
		promiseFactory().eventually { factory -> factory.promiseServerClient(connectionDetails) }

	override fun promiseStreamingServerClient(connectionDetails: MediaCenterConnectionDetails): Promise<HttpPromiseClient> =
		promiseFactory().eventually { factory -> factory.promiseStreamingServerClient(connectionDetails) }

	private fun promiseFactory() = applicationFeatureConfiguration
		.promiseFeatureConfiguration()
		.then { features ->
			val clientType = features?.httpClientType ?: HttpClientType.OkHttp
			when (clientType) {
				HttpClientType.OkHttp -> okHttpClient
				HttpClientType.Ktor -> ktorClient
			}
		}
}
