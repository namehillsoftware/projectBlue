package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders;
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.resources.network.ActiveNetworkFinder;
import com.lasthopesoftware.resources.strings.Base64Encoder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class AccessConfigurationBuilder {

	private static final int buildConnectionTimeoutTime = 10000;

	private static final CreateAndHold<BuildUrlProviders> lazyUrlScanner = new AbstractSynchronousLazy<BuildUrlProviders>() {
		@Override
		protected BuildUrlProviders create() {
			final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(buildConnectionTimeoutTime, TimeUnit.MILLISECONDS).build();
			final ServerLookup serverLookup = new ServerLookup(new ServerInfoXmlRequest(client));
			final ConnectionTester connectionTester = new ConnectionTester();

			return new UrlScanner(new Base64Encoder(), connectionTester, serverLookup, OkHttpFactory.getInstance());
		}
	};

	public static Promise<IUrlProvider> buildConfiguration(final Context context, final Library library) {
		return new LiveUrlProvider(
			new ActiveNetworkFinder(context),
			lazyUrlScanner.getObject()).promiseLiveUrl(library);
	}
}
