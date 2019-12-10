package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.content.Context;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor;
import com.namehillsoftware.lazyj.Lazy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Dispatcher;

public class HttpDataSourceFactoryProvider implements ProvideHttpDataSourceFactory {

	private static final Lazy<ExecutorService> lazyExecutor = new Lazy<>(CachedSingleThreadExecutor::new);

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
				.dispatcher(new Dispatcher(lazyExecutor.getObject()))
				.build(),
			Util.getUserAgent(context, context.getString(R.string.app_name)));
	}
}
