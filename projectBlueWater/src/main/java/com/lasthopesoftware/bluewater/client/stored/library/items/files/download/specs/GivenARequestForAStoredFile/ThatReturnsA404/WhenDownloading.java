package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.specs.GivenARequestForAStoredFile.ThatReturnsA404;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenDownloading {

	private static InputStream inputStream;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(404, new byte[0]));

		final StoredFileDownloader downloader = new StoredFileDownloader();
		inputStream = new FuturePromise<>(downloader.promiseDownload(new StoredFile().setServiceId(4))).get();
	}

	@Test
	public void thenAnEmptyInputStreamIsReturned() throws IOException {
		assertThat(inputStream.available()).isEqualTo(0);
	}
}
