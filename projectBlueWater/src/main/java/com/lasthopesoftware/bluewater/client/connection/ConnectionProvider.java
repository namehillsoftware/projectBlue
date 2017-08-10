package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersion;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider;
import com.lasthopesoftware.messenger.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionProvider implements IConnectionProvider {

	private final IUrlProvider urlProvider;
	private final ILazy<Promise<ProgramVersion>> lazyPromisedProgramVersion = new AbstractSynchronousLazy<Promise<ProgramVersion>>() {
		@Override
		protected Promise<ProgramVersion> initialize() throws Exception {
			final ProgramVersionProvider programVersionProvider = new ProgramVersionProvider(ConnectionProvider.this);
			return programVersionProvider.promiseServerVersion();
		}
	};

	public ConnectionProvider(IUrlProvider urlProvider) {
		this.urlProvider = urlProvider;
	}

	@Override
	public HttpURLConnection getConnection(String... params) throws IOException {
		if (urlProvider == null) return null;

		final URL url = new URL(urlProvider.getUrl(params));
		final String authCode = urlProvider.getAuthCode();

		final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(180000);

		if (authCode != null && !authCode.isEmpty())
			connection.setRequestProperty("Authorization", "basic " + authCode);

		return connection;
	}

	public IUrlProvider getUrlProvider() {
		return urlProvider;
	}

	@Override
	public Promise<ProgramVersion> getConnectionProgramVersion() {
		return lazyPromisedProgramVersion.getObject();
	}
}
