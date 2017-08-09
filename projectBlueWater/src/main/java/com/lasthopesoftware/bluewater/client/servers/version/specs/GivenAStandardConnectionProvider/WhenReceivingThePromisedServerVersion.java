package com.lasthopesoftware.bluewater.client.servers.version.specs.GivenAStandardConnectionProvider;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.servers.version.ProgramVersion;
import com.lasthopesoftware.bluewater.client.servers.version.ServerVersionProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenReceivingThePromisedServerVersion {

	private static ProgramVersion version;

	@BeforeClass
	public static void before() throws IOException {
		final IConnectionProvider urlProvider =  mock(IConnectionProvider.class);
		final HttpURLConnection urlConnection = mock(HttpURLConnection.class);
		when(urlConnection.getInputStream())
			.thenReturn(new ByteArrayInputStream("<Response Status=\"OK\"><Item Name=\"RuntimeGUID\">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item><Item Name=\"LibraryVersion\">24</Item><Item Name=\"ProgramName\">JRiver Media Center</Item><Item Name=\"ProgramVersion\">22.0.108</Item><Item Name=\"FriendlyName\">Media-Pc</Item><Item Name=\"AccessKey\">nIpfQr</Item></Response>".getBytes()));
		when(urlProvider.getConnection("Alive")).thenReturn(urlConnection);

		final ServerVersionProvider serverVersionProviderProvider = new ServerVersionProvider(urlProvider);
		serverVersionProviderProvider.promiseServerVersion().then(v -> version = v);
	}

	@Test
	public void thenTheServerVersionIsCorrect() {
		assertThat(version).isEqualTo(new ProgramVersion(22, 0, 108));
	}
}
