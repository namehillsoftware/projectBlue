package com.lasthopesoftware.bluewater.client.connection.specs;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

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
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(mappedResponse.resultFrom(params));
			when(mockConnection.getInputStream()).thenReturn(inputStream);
		} catch (Throwable throwable) {
			when(mockConnection.getInputStream()).thenThrow(throwable);
		}

		when(mockConnection.getResponseCode()).thenReturn(200);

		return mockConnection;
	}

	@Override
	public X509TrustManager getTrustManager() {
		return mock(X509TrustManager.class);
	}

	@Override
	public SSLSocketFactory getSslSocketFactory() {
		return mock(SSLSocketFactory.class);
	}

	@Override
	public IUrlProvider getUrlProvider() {
		try {
			return new MediaServerUrlProvider(null, "test", 80);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
