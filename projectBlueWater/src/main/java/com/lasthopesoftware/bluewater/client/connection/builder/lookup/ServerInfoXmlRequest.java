package com.lasthopesoftware.bluewater.client.connection.builder.lookup;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.HttpPromisedResponse;
import com.namehillsoftware.handoff.promises.Promise;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import xmlwise.XmlElement;
import xmlwise.Xmlwise;

public class ServerInfoXmlRequest implements RequestServerInfoXml {

	private final OkHttpClient client;

	public ServerInfoXmlRequest(OkHttpClient client) {
		this.client = client;
	}

	@Override
	public Promise<XmlElement> promiseServerInfoXml(Library library) {
		final Request request = new Request.Builder()
			.url("https://webplay.jriver.com/libraryserver/lookup?id=" + library.getAccessCode())
			.build();

		return new HttpPromisedResponse(client.newCall(request))
			.then(response -> {
				final ResponseBody body = response.body();
				if (body == null) return null;

				try {
					return Xmlwise.createXml(body.string());
				} finally {
					body.close();
				}
			});
	}
}
