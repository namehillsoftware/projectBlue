package com.lasthopesoftware.bluewater.client.connection.builder.live.specs.GivenANetworkDoesNotExist;

import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheLiveUrl {

	private static IUrlProvider urlProvider;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final LiveUrlProvider liveUrlProvider = new LiveUrlProvider(
			() -> null,
			(library) -> {
				final IUrlProvider urlProvider = mock(IUrlProvider.class);
				when(urlProvider.getBaseUrl()).thenReturn("http://test-url");
				return new Promise<>(urlProvider);
			});
		urlProvider = new FuturePromise<>(liveUrlProvider.promiseLiveUrl(new Library())).get();
	}

	@Test
	public void thenAUrlProviderIsNotReturned() {
		assertThat(urlProvider).isNull();
	}
}
