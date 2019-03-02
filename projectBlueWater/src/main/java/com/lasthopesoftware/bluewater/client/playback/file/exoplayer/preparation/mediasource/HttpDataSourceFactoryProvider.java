package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.content.Context;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import java.util.concurrent.TimeUnit;

public class HttpDataSourceFactoryProvider implements ProvideHttpDataSourceFactory {

	private final Context context;
	private final IConnectionProvider connectionProvider;
	private final ProvideOkHttpClients okHttpClients;

	public HttpDataSourceFactoryProvider(Context context, IConnectionProvider connectionProvider, ProvideOkHttpClients okHttpClients) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.okHttpClients = okHttpClients;
	}

	@Override
	public HttpDataSource.Factory getHttpDataSourceFactory(Library library) {
		return new OkHttpDataSourceFactory(
			okHttpClients.getOkHttpClient(connectionProvider.getUrlProvider()).newBuilder()
				.readTimeout(45, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false)
				.build(),
			Util.getUserAgent(context, context.getString(R.string.app_name)));
	}
}
