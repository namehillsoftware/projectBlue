package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.content.Context
import androidx.media3.common.util.Util
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.TimeUnit

class HttpDataSourceFactoryProvider(
	private val context: Context,
	private val connectionProvider: ProvideGuaranteedLibraryConnections,
	private val okHttpClients: ProvideOkHttpClients
) : ProvideHttpDataSourceFactory {

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun promiseHttpDataSourceFactory(libraryId: LibraryId): Promise<HttpDataSource.Factory> =
		connectionProvider
			.promiseLibraryConnection(libraryId)
			.then { it ->
				OkHttpDataSource.Factory(
					okHttpClients.getOkHttpClient(it.urlProvider)
						.newBuilder()
						.readTimeout(45, TimeUnit.SECONDS)
						.retryOnConnectionFailure(false)
						.build())
					.setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)))
			}
}
