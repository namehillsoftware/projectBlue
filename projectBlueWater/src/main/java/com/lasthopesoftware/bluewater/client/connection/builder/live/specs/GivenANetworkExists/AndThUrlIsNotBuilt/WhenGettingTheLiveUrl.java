package com.lasthopesoftware.bluewater.client.connection.builder.live.specs.GivenANetworkExists.AndThUrlIsNotBuilt;

import android.net.NetworkInfo;
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

public class WhenGettingTheLiveUrl {

	private static IUrlProvider urlProvider;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final LiveUrlProvider liveUrlProvider = new LiveUrlProvider(
			() -> mock(NetworkInfo.class),
			(library) -> Promise.empty());
		urlProvider = new FuturePromise<>(liveUrlProvider.promiseLiveUrl(new Library())).get();
	}

	@Test
	public void thenTheUrlIsCorrect() {
		assertThat(urlProvider).isNull();
	}
}
