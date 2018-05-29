package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import android.net.NetworkInfo;

import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

public class AccessConfigurationBuilder {

	private static final int buildConnectionTimeoutTime = 10000;

	public static Promise<IUrlProvider> buildConfiguration(final Context context, final Library library) {
		final NetworkInfo networkInfo = ConnectionInfo.getActiveNetworkInfo(context);
		if (networkInfo != null && networkInfo.isConnected()) return Promise.empty();

		final ServerLookup serverLookup = new ServerLookup(new ServerInfoXmlRequest(Duration.millis(buildConnectionTimeoutTime)));
		final ConnectionTester connectionTester = new ConnectionTester(Duration.millis(buildConnectionTimeoutTime));

		final UrlScanner urlScanner = new UrlScanner(connectionTester, serverLookup);
		return urlScanner.promiseBuiltUrlProvider(library);
	}
}
