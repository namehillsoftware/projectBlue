package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.content.Context;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.TimeUnit;

public class HttpDataSourceFactoryProvider implements ProvideHttpDataSourceFactory {

	private final Context context;
	private final SSLSocketFactory sslSocketFactory;
	private final X509TrustManager trustManager;

	public HttpDataSourceFactoryProvider(Context context, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
		this.context = context;
		this.sslSocketFactory = sslSocketFactory;
		this.trustManager = trustManager;
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
				.sslSocketFactory(sslSocketFactory, trustManager)
				.build(),
			Util.getUserAgent(context, context.getString(R.string.app_name)),
			null);

		final String authKey = library.getAuthKey();

		if (authKey != null && !authKey.isEmpty())
			httpDataSourceFactory.getDefaultRequestProperties().set("Authorization", "basic " + authKey);

		return httpDataSourceFactory;
	}
}
