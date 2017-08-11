package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface IConnectionProvider {
	HttpURLConnection getConnection(String... params) throws IOException;
	IUrlProvider getUrlProvider();
	Promise<SemanticVersion> promiseConnectionProgramVersion();
}
