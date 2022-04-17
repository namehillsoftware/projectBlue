package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import android.content.Context
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.resources.executors.ThreadPools
import okhttp3.Dispatcher
import java.util.concurrent.TimeUnit

class HttpDataSourceFactoryProvider(
	private val context: Context,
	private val connectionProvider: IConnectionProvider,
	private val okHttpClients: ProvideOkHttpClients
) : ProvideHttpDataSourceFactory {

	companion object {
		private val constrainedDispatcher by lazy {
			Dispatcher(ThreadPools.io).apply { maxRequests = 2 }
		}
	}

	private val factory by lazy {
		OkHttpDataSource.Factory(
			okHttpClients.getOkHttpClient(connectionProvider.urlProvider).newBuilder()
				.readTimeout(45, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false)
				.dispatcher(constrainedDispatcher)
				.build())
			.setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)))
	}

	override fun getHttpDataSourceFactory(): HttpDataSource.Factory = factory
}
