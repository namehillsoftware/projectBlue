package com.lasthopesoftware.bluewater.client.connection.specs;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
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
	private final HashMap<Set<String>, CarelessOneParameterFunction<String[], byte[]>> mappedResponses = new HashMap<>();

	public final void mapResponse(CarelessOneParameterFunction<String[], byte[]> response, String... params) {
		final HashSet<String> paramsSet = new HashSet<>(Arrays.asList(params));
		mappedResponses.put(paramsSet, response);
	}

	@Override
	public HttpURLConnection getConnection(String... params) throws IOException {
		final HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getResponseCode()).thenReturn(404);

		CarelessOneParameterFunction<String[], byte[]> mappedResponse = mappedResponses.get(new HashSet<>(Arrays.asList(params)));

		if (mappedResponse == null) {
			final Optional<Set<String>> optionalResponse = Stream.of(mappedResponses.keySet())
				.filter(set -> Stream.of(set).allMatch(sp -> Stream.of(params).anyMatch(p -> p.matches(sp))))
				.findFirst();

			if (optionalResponse.isPresent())
				mappedResponse = mappedResponses.get(optionalResponse.get());
		}

		if (mappedResponse == null) return mockConnection;

		try {
			when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(mappedResponse.resultFrom(params)));
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
