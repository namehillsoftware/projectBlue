package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;
import android.net.NetworkInfo;

import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders;
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.joda.time.Duration;

public class AccessConfigurationBuilder {

	private static final int buildConnectionTimeoutTime = 10000;

	private static final CreateAndHold<BuildUrlProviders> lazyUrlScanner = new AbstractSynchronousLazy<BuildUrlProviders>() {
		@Override
		protected BuildUrlProviders create() {
			final ServerLookup serverLookup = new ServerLookup(new ServerInfoXmlRequest(Duration.millis(buildConnectionTimeoutTime)));
			final ConnectionTester connectionTester = new ConnectionTester(Duration.millis(buildConnectionTimeoutTime));

			return new UrlScanner(connectionTester, serverLookup);
		}
	};

	public static Promise<IUrlProvider> buildConfiguration(final Context context, final Library library) {
		final NetworkInfo networkInfo = ConnectionInfo.getActiveNetworkInfo(context);
		if (networkInfo != null && networkInfo.isConnected()) return Promise.empty();

		return lazyUrlScanner.getObject().promiseBuiltUrlProvider(library);
	}
}
