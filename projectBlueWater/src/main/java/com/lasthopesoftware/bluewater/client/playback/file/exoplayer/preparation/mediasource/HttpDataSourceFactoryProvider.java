package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.content.Context;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;

public class HttpDataSourceFactoryProvider implements ProvideHttpDataSourceFactory {

	private final Context context;
	private final IConnectionProvider connectionProvider;

	public HttpDataSourceFactoryProvider(Context context, IConnectionProvider connectionProvider) {
		this.context = context;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public HttpDataSource.Factory getHttpDataSourceFactory(Library library) {
		final OkHttpDataSourceFactory httpDataSourceFactory = new OkHttpDataSourceFactory(
			new OkHttpClient.Builder()
				.readTimeout(45, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false)
				.addNetworkInterceptor(chain -> {
					Request request = chain.request().newBuilder().addHeader("Connection", "close").build();
					return chain.proceed(request);
				})
				.sslSocketFactory(connectionProvider.getSslSocketFactory(), connectionProvider.getTrustManager())
				.hostnameVerifier(connectionProvider.getHostnameVerifier())
				.build(),
			Util.getUserAgent(context, context.getString(R.string.app_name)));

		final String authKey = library.getAuthKey();

		if (authKey != null && !authKey.isEmpty())
			httpDataSourceFactory.getDefaultRequestProperties().set("Authorization", "basic " + authKey);

		return httpDataSourceFactory;
	}
}
