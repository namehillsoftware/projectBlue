package com.lasthopesoftware.bluewater.client.library.access.specs;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FakeRevisionConnectionProvider implements IConnectionProvider {

	private int syncRevision;

	@Override
	public HttpURLConnection getConnection(String... params) throws IOException {
		final HttpURLConnection mockConnection = mock(HttpURLConnection.class);

		if (params.length < 0) return mockConnection;

		if (params[0].equals("Library/GetRevision")) {
			when(mockConnection.getInputStream())
				.thenReturn(new ByteArrayInputStream(
					("<Response Status=\"OK\">" +
						"<Item Name=\"Master\">1192</Item>" +
						"<Item Name=\"Sync\">" + syncRevision + "</Item>" +
						"<Item Name=\"LibraryStartup\">1501430846</Item>" +
					"</Response>").getBytes()));
		}

		return null;
	}

	@Override
	public IUrlProvider getUrlProvider() {
		return null;
	}

	@Override
	public Promise<SemanticVersion> promiseConnectionProgramVersion() {
		return new Promise<>(new SemanticVersion(1, 0, 0));
	}

	public void setSyncRevision(int syncRevision) {
		this.syncRevision = syncRevision;
	}
}
