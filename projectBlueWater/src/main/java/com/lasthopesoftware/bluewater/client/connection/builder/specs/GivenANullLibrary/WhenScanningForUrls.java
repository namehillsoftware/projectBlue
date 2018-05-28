package com.lasthopesoftware.bluewater.client.connection.builder.specs.GivenANullLibrary;

import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenScanningForUrls {

	private static IllegalArgumentException illegalArgumentException;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final UrlScanner urlScanner = new UrlScanner();

		try {
			new FuturePromise<>(urlScanner.promiseBuiltUrlProvider(null)).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IllegalArgumentException)
				illegalArgumentException = (IllegalArgumentException) e.getCause();
			else
				throw e;
		}
	}

	@Test
	public void thenAnIllegalArgumentExceptionIsThrown() {
		assertThat(illegalArgumentException).isNotNull();
	}

	@Test
	public void thenTheExceptionMentionsTheLibrary() {
		assertThat(illegalArgumentException.getMessage()).isEqualTo("The library cannot be null");
	}
}
