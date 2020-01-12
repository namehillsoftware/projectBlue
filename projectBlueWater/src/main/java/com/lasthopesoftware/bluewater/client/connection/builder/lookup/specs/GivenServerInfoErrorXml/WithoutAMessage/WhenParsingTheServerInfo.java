package com.lasthopesoftware.bluewater.client.connection.builder.lookup.specs.GivenServerInfoErrorXml.WithoutAMessage;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.RequestServerInfoXml;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerDiscoveryException;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenParsingTheServerInfo {

	private static ServerDiscoveryException exception;

	@BeforeClass
	public static void before() throws XmlParseException, ExecutionException, InterruptedException {
		final RequestServerInfoXml serverInfoXml = mock(RequestServerInfoXml.class);
		when(serverInfoXml.promiseServerInfoXml(any()))
			.thenReturn(new Promise<>(Xmlwise.createXml(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<Response Status=\"Error\">\n" +
				"</Response>")));

		final ServerLookup serverLookup = new ServerLookup(serverInfoXml);

		try {
			new FuturePromise<>(serverLookup.promiseServerInformation(new Library())).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ServerDiscoveryException)
				exception = (ServerDiscoveryException)e.getCause();
			else throw e;
		}
	}

	@Test
	public void thenAServerDiscoveryExceptionIsThrownWithTheCorrectMessage() {
		assertThat(exception).isNotNull();
	}
}
