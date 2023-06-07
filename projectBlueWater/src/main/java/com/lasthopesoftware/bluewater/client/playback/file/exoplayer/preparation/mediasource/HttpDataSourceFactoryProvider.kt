package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.content.Context
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Dispatcher
import java.util.concurrent.TimeUnit

class HttpDataSourceFactoryProvider(
	private val context: Context,
	private val connectionProvider: ProvideGuaranteedLibraryConnections,
	private val okHttpClients: ProvideOkHttpClients
) : ProvideHttpDataSourceFactory {

	companion object {
		private val constrainedDispatcher by lazy {
			Dispatcher(ThreadPools.io).apply { maxRequests = 2 }
		}
	}

	override fun promiseHttpDataSourceFactory(libraryId: LibraryId): Promise<HttpDataSource.Factory> =
		connectionProvider
			.promiseLibraryConnection(libraryId)
			.then {
				OkHttpDataSource.Factory(
					okHttpClients.getOkHttpClient(it.urlProvider)
						.newBuilder()
						.readTimeout(45, TimeUnit.SECONDS)
						.retryOnConnectionFailure(false)
						.dispatcher(constrainedDispatcher)
						.build())
					.setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)))
			}
}
