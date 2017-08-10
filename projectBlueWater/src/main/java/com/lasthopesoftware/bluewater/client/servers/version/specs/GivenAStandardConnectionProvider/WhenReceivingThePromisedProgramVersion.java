package com.lasthopesoftware.bluewater.client.servers.version.specs.GivenAStandardConnectionProvider;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersion;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersionProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenReceivingThePromisedProgramVersion {

	private static ProgramVersion version;
	private static ProgramVersion expectedVersion;

	@BeforeClass
	public static void before() throws IOException {
		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");

		final IConnectionProvider connectionProvider = mock(IConnectionProvider.class);
		when(connectionProvider.getUrlProvider()).thenReturn(urlProvider);

		final HttpURLConnection urlConnection = mock(HttpURLConnection.class);

		final Random random = new Random();
		expectedVersion = new ProgramVersion(random.nextInt(), random.nextInt(), random.nextInt());
		when(urlConnection.getInputStream())
			.thenReturn(new ByteArrayInputStream(
				("<Response Status=\"OK\">" +
					"<Item Name=\"RuntimeGUID\">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item>" +
					"<Item Name=\"LibraryVersion\">24</Item><Item Name=\"ProgramName\">JRiver Media Center</Item>" +
					"<Item Name=\"ProgramVersion\">" + expectedVersion + "</Item>" +
					"<Item Name=\"FriendlyName\">Media-Pc</Item>" +
					"<Item Name=\"AccessKey\">nIpfQr</Item>" +
				"</Response>").getBytes()));
		when(connectionProvider.getConnection("Alive")).thenReturn(urlConnection);

		final ProgramVersionProvider programVersionProvider = new ProgramVersionProvider(connectionProvider);
		programVersionProvider.promiseServerVersion().then(v -> version = v);
	}

	@Test
	public void thenTheServerVersionIsCorrect() {
		assertThat(version).isEqualTo(expectedVersion);
	}
}
