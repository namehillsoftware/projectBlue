package com.lasthopesoftware.bluewater.client.connection.specs;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FakeConnectionProvider implements IConnectionProvider {
	private final HashMap<Set<String>, CarelessOneParameterFunction<String[], String>> mappedResponses = new HashMap<>();

	public final void mapResponse(CarelessOneParameterFunction<String[], String> response, String... params) {
		mappedResponses.put(new HashSet<>(Arrays.asList(params)), response);
	}

	@Override
	public HttpURLConnection getConnection(String... params) throws IOException {
		final HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getResponseCode()).thenReturn(404);

		final CarelessOneParameterFunction<String[], String> mappedResponse = mappedResponses.get(new HashSet<>(Arrays.asList(params)));

		if (mappedResponse == null) return mockConnection;

		try {
			when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(mappedResponse.resultFrom(params).getBytes()));
		} catch (Throwable throwable) {
			throw new IOException(throwable);
		}

		when(mockConnection.getResponseCode()).thenReturn(200);

		return mockConnection;
	}

	@Override
	public IUrlProvider getUrlProvider() {
		final IUrlProvider urlProvider = mock(IUrlProvider.class);
		when(urlProvider.getBaseUrl()).thenReturn("");
		return urlProvider;
	}

}
