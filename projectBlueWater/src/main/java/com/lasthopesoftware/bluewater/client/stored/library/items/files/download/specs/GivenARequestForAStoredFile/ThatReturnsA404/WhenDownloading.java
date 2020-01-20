package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.specs.GivenARequestForAStoredFile.ThatReturnsA404;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeLibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenDownloading {

	private static InputStream inputStream;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakeLibraryConnectionProvider fakeConnectionProvider = new FakeLibraryConnectionProvider(new HashMap<LibraryId, IConnectionProvider>() {{
			put(new LibraryId(2), new FakeConnectionProvider());
		}});

		final StoredFileDownloader downloader = new StoredFileDownloader(new ServiceFileUriQueryParamsProvider(), fakeConnectionProvider);
		inputStream = new FuturePromise<>(downloader.promiseDownload(new LibraryId(2), new StoredFile().setServiceId(4))).get();
	}

	@Test
	public void thenAnEmptyInputStreamIsReturned() throws IOException {
		assertThat(inputStream.available()).isEqualTo(0);
	}
}
