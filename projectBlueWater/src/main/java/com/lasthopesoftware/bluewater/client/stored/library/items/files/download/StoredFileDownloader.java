package com.lasthopesoftware.bluewater.client.stored.library.items.files.download;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy;
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class StoredFileDownloader implements DownloadStoredFiles {

	@NonNull private final IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider;

	@NonNull private final IConnectionProvider connectionProvider;

	public StoredFileDownloader(@NonNull IServiceFileUriQueryParamsProvider serviceFileUriQueryParamsProvider, @NonNull IConnectionProvider connectionProvider) {
		this.serviceFileUriQueryParamsProvider = serviceFileUriQueryParamsProvider;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Promise<InputStream> promiseDownload(StoredFile storedFile) {
		return new Promise<>(m -> {
			final CancellationProxy cancellationProxy = new CancellationProxy();
			m.cancellationRequested(cancellationProxy);

			final Promise<Response> promisedResponse = connectionProvider
				.promiseResponse(serviceFileUriQueryParamsProvider.getServiceFileUriQueryParams(new ServiceFile(storedFile.getServiceId())));

			cancellationProxy.doCancel(promisedResponse);

			promisedResponse
				.then(r -> r.body() == null || r.code() == 404
					? new ByteArrayInputStream(new byte[0])
					: new StreamedResponse(r.body()))
				.then(
					new ResolutionProxy<>(m),
					new RejectionProxy(m));
		});
	}

	private static final class StreamedResponse extends InputStream {

		private final ResponseBody responseBody;
		private final InputStream byteStream;

		StreamedResponse(ResponseBody responseBody) {
			this.responseBody = responseBody;
			byteStream = this.responseBody.byteStream();
		}

		@Override
		public int read() throws IOException {
			return byteStream.read();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return byteStream.read(b, off, len);
		}

		@Override
		public int available() throws IOException {
			return byteStream.available();
		}

		@Override
		public void close() throws IOException {
			byteStream.close();
			responseBody.close();
		}

		@Override
		public String toString() {
			return byteStream.toString();
		}
	}
}
