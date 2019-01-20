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

import java.io.ByteArrayInputStream;
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
					: r.body().byteStream())
				.then(
					new ResolutionProxy<>(m),
					new RejectionProxy(m));
		});
	}
}
