package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.specs.GivenARequestForAStoredFile.ThatSucceeds;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionResponseTuple;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenDownloading {

	private static final byte[] responseBytes = new byte[400];

	static {
		Random random = new Random();
		random.nextBytes(responseBytes);
	}

	private static InputStream inputStream;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(p -> new FakeConnectionResponseTuple(200, responseBytes));

		final StoredFileDownloader downloader = new StoredFileDownloader(new ServiceFileUriQueryParamsProvider(), fakeConnectionProvider);
		inputStream = new FuturePromise<>(downloader.promiseDownload(job.getLibraryId(), new StoredFile().setServiceId(4))).get();
	}

	@Test
	public void thenTheInputStreamIsReturned() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		IOUtils.copy(inputStream, outputStream);
		assertThat(outputStream.toByteArray()).containsExactly(responseBytes);
	}
}
