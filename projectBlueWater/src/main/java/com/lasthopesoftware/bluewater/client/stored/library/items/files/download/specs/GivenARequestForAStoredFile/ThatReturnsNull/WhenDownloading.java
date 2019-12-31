package com.lasthopesoftware.bluewater.client.stored.library.items.files.download.specs.GivenARequestForAStoredFile.ThatReturnsNull;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenDownloading {

	private static InputStream inputStream;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final Request.Builder builder = new Request.Builder();
		builder.url("http://stuff/");

		final Response.Builder responseBuilder = new Response.Builder();
		responseBuilder
			.request(builder.build())
			.protocol(Protocol.HTTP_1_1)
			.code(202)
			.message("Not Found")
			.body(null);

		final IConnectionProvider fakeConnectionProvider = mock(IConnectionProvider.class);
		when(fakeConnectionProvider.promiseResponse(any())).thenReturn(new Promise<>(responseBuilder.build()));

		final StoredFileDownloader downloader = new StoredFileDownloader(new ServiceFileUriQueryParamsProvider(), fakeConnectionProvider);
		inputStream = new FuturePromise<>(downloader.promiseDownload(job.getLibraryId(), new StoredFile().setServiceId(4))).get();
	}

	@Test
	public void thenAnEmptyInputStreamIsReturned() throws IOException {
		assertThat(inputStream.available()).isEqualTo(0);
	}
}
