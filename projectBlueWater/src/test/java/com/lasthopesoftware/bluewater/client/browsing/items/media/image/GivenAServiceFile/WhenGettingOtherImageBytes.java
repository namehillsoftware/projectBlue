package com.lasthopesoftware.bluewater.client.browsing.items.media.image.GivenAServiceFile;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.RemoteImageAccess;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.connection.FakeLibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingOtherImageBytes {

	private static byte[] imageBytes;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(
			p -> new FakeConnectionResponseTuple(200, new byte[] { 46, 78, 99, 42 }),
			"File/GetImage", "File=583", "Type=Full", "Pad=1", "Format=jpg", "FillTransparency=ffffff");

		final RemoteImageAccess memoryCachedImageAccess = new RemoteImageAccess(
			new FakeLibraryConnectionProvider(
				new HashMap<LibraryId, IConnectionProvider>() {
					{
						put(new LibraryId(11), fakeConnectionProvider);
					}
				}
			)
		);

		imageBytes = new FuturePromise<>(memoryCachedImageAccess.promiseImageBytes(
			new LibraryId(11), new ServiceFile(583))).get();
	}

	@Test
	public void thenTheBytesAreCorrect() {
		assertThat(imageBytes).isEqualTo(new byte[] { 46, 78, 99, 42 });
	}
}
