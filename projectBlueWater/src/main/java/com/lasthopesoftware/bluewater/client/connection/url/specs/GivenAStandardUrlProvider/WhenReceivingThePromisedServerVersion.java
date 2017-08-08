package com.lasthopesoftware.bluewater.client.connection.url.specs.GivenAStandardUrlProvider;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.servers.ProgramVersion;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenReceivingThePromisedServerVersion {

	private static ProgramVersion version;

	@BeforeClass
	public static void before() {
		final IUrlProvider urlProvider =  mock(IUrlProvider.class);
		when(urlProvider.getUrl("Alive")).thenReturn("<Response Status=\"OK\"><Item Name=\"RuntimeGUID\">{7FF5918E-9FDE-4D4D-9AE7-62DFFDD64397}</Item><Item Name=\"LibraryVersion\">24</Item><Item Name=\"ProgramName\">JRiver Media Center</Item><Item Name=\"ProgramVersion\">22.0.108</Item><Item Name=\"FriendlyName\">Media-Pc</Item><Item Name=\"AccessKey\">nIpfQr</Item></Response>");

		final ConnectionProvider connectionProvider = new ConnectionProvider(urlProvider);
		connectionProvider.promiseServerVersion().then(v -> version = v);
	}

	@Test
	public void thenTheServerVersionIsCorrect() {
		assertThat(version).isEqualTo(new ProgramVersion(22, 0, 108, null));
	}
}
