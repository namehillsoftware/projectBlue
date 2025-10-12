package com.lasthopesoftware.bluewater.client.connection.okhttp

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClientOptions
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.namehillsoftware.handoff.promises.Promise

class ApplicationSettingsHttpClient(
	private val applicationSettings: HoldApplicationSettings,
	private val okHttpFactory: OkHttpFactory,
	private val ktorFactory: KtorFactory,
) : ProvideHttpPromiseClients {
	override fun promiseServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails): Promise<HttpPromiseClient> =
		promiseFactory()
			.eventually { factory ->
				factory.promiseServerClient(mediaCenterConnectionDetails)
			}

	override fun promiseServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails, clientOptions: HttpPromiseClientOptions): Promise<HttpPromiseClient> =
		promiseFactory()
			.eventually { factory ->
				factory.promiseServerClient(mediaCenterConnectionDetails, clientOptions)
			}

	override fun promiseServerClient(subsonicConnectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient> =
		promiseFactory()
			.eventually { factory ->
				factory.promiseServerClient(subsonicConnectionDetails)
			}

	override fun promiseClient(): Promise<HttpPromiseClient> =
		promiseFactory()
			.eventually { factory ->
				factory.promiseClient()
			}

	override fun promiseServerClient(subsonicConnectionDetails: SubsonicConnectionDetails, clientOptions: HttpPromiseClientOptions): Promise<HttpPromiseClient> =
		promiseFactory()
			.eventually { factory ->
				factory.promiseServerClient(subsonicConnectionDetails, clientOptions)
			}


	private fun promiseFactory() = applicationSettings
		.promiseApplicationSettings()
		.then { settings ->
			val clientType = settings?.httpClientTypeName?.let(HttpClientType::valueOf) ?: HttpClientType.OkHttp
			when (clientType) {
				HttpClientType.OkHttp -> okHttpFactory
				HttpClientType.Ktor -> ktorFactory
			}
		}
}
