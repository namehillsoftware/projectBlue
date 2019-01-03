package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.Response;

public interface IConnectionProvider {
	Promise<Response> promiseResponse(String... params);

	IUrlProvider getUrlProvider();
}
