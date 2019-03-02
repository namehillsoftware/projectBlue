package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.MalformedURLException;
import java.net.URL;

public class ConnectionProvider implements IConnectionProvider {

	private final IUrlProvider urlProvider;
	private final ProvideOkHttpClients okHttpClients;

	private final CreateAndHold<OkHttpClient> lazyOkHttpClient = new AbstractSynchronousLazy<OkHttpClient>() {
		@Override
		protected OkHttpClient create() {
			return okHttpClients.getOkHttpClient(urlProvider);
		}
	};

	public ConnectionProvider(IUrlProvider urlProvider, ProvideOkHttpClients okHttpClients) {
		if (urlProvider == null) throw new IllegalArgumentException("urlProvider != null");
		this.urlProvider = urlProvider;

		if (okHttpClients == null) throw new IllegalArgumentException("okHttpClients != null");
		this.okHttpClients = okHttpClients;
	}

	@Override
	public Promise<Response> promiseResponse(String... params) {
		try {
			return new HttpPromisedResponse(callServer(params));
		} catch (Throwable e) {
			return new Promise<>(e);
		}
	}

	public IUrlProvider getUrlProvider() {
		return urlProvider;
	}

	private Call callServer(String... params) throws MalformedURLException {
		final URL url = new URL(urlProvider.getUrl(params));

		final Request request = new Request.Builder().url(url).build();
		return lazyOkHttpClient.getObject().newCall(request);
	}
}
