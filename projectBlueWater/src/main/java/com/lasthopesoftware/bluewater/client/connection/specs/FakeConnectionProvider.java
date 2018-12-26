package com.lasthopesoftware.bluewater.client.connection.specs;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FakeConnectionProvider implements IConnectionProvider {
	private final HashMap<Set<String>, CarelessOneParameterFunction<String[], ResponseTuple>> mappedResponses = new HashMap<>();

	public final void mapResponse(CarelessOneParameterFunction<String[], ResponseTuple> response, String... params) {
		final HashSet<String> paramsSet = new HashSet<>(Arrays.asList(params));
		mappedResponses.put(paramsSet, response);
	}

	@Override
	public HttpURLConnection getConnection(String... params) throws IOException {
		final HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getResponseCode()).thenReturn(404);

		CarelessOneParameterFunction<String[], ResponseTuple> mappedResponse = mappedResponses.get(new HashSet<>(Arrays.asList(params)));

		if (mappedResponse == null) {
			final Optional<Set<String>> optionalResponse = Stream.of(mappedResponses.keySet())
				.filter(set -> Stream.of(set).allMatch(sp -> Stream.of(params).anyMatch(p -> p.matches(sp))))
				.findFirst();

			if (optionalResponse.isPresent())
				mappedResponse = mappedResponses.get(optionalResponse.get());
		}

		if (mappedResponse == null) return mockConnection;

		try {
			final ResponseTuple entry = mappedResponse.resultFrom(params);
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(entry.response);
			when(mockConnection.getInputStream()).thenReturn(inputStream);
			when(mockConnection.getResponseCode()).thenReturn(entry.code);
		} catch (Throwable throwable) {
			when(mockConnection.getInputStream()).thenThrow(throwable);
		}


		return mockConnection;
	}

	@Override
	public OkHttpClient getClient() {
		return null;
	}

	@Override
	public Promise<Response> promiseResponse(String... params) {
		try {
			return new Promise<>(getResponse(params));
		} catch (IOException e) {
			return new Promise<>(e);
		}
	}

	@Override
	public Response getResponse(String... params) throws IOException {
		final Request.Builder builder = new Request.Builder();
		builder.url(getUrlProvider().getUrl(params));

		final Buffer buffer = new Buffer();

		final Response.Builder responseBuilder = new Response.Builder();
		responseBuilder
			.request(builder.build())
			.protocol(Protocol.HTTP_1_1)
			.message("Not Found")
			.body(new RealResponseBody(null, 0, buffer))
			.code(404);

		CarelessOneParameterFunction<String[], ResponseTuple> mappedResponse = mappedResponses.get(new HashSet<>(Arrays.asList(params)));

		if (mappedResponse == null) {
			final Optional<Set<String>> optionalResponse = Stream.of(mappedResponses.keySet())
				.filter(set -> Stream.of(set).allMatch(sp -> Stream.of(params).anyMatch(p -> p.matches(sp))))
				.findFirst();

			if (optionalResponse.isPresent())
				mappedResponse = mappedResponses.get(optionalResponse.get());
		}

		if (mappedResponse == null) return responseBuilder.build();

		try {
			final ResponseTuple result = mappedResponse.resultFrom(params);
			buffer.write(result.response);
			responseBuilder.code(result.code);
			responseBuilder.body(new RealResponseBody(null, result.response.length, buffer));
		} catch (IOException io) {
			throw io;
		} catch (Throwable ignored) {
		}

		return responseBuilder.build();
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

	@Override
	public HostnameVerifier getHostnameVerifier() {
		return mock(HostnameVerifier.class);
	}

	public static class ResponseTuple {
		public final int code;
		public final byte[] response;

		public ResponseTuple(int code, byte[] response) {
			this.code = code;
			this.response = response;
		}
	}
}
