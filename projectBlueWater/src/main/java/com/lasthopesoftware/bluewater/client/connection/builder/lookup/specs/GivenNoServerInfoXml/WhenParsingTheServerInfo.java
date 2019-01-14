package com.lasthopesoftware.bluewater.client.connection.builder.lookup.specs.GivenNoServerInfoXml;

import com.lasthopesoftware.bluewater.client.connection.builder.lookup.RequestServerInfoXml;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenParsingTheServerInfo {

	private static ServerInfo serverInfo;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final RequestServerInfoXml serverInfoXml = mock(RequestServerInfoXml.class);
		when(serverInfoXml.promiseServerInfoXml(any()))
			.thenReturn(Promise.empty());

		final ServerLookup serverLookup = new ServerLookup(serverInfoXml);
		serverInfo = new FuturePromise<>(serverLookup.promiseServerInformation(new Library())).get();
	}

	@Test
	public void thenNoServerInfoIsReturned() {
		assertThat(serverInfo).isNull();
	}
}
