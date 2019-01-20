package com.lasthopesoftware.bluewater.client.stored.library.items.files.download;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

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
		return connectionProvider
			.promiseResponse(serviceFileUriQueryParamsProvider.getServiceFileUriQueryParams(new ServiceFile(storedFile.getServiceId())))
			.then(r -> r.body() == null || r.code() == 404
				? new ByteArrayInputStream(new byte[0])
				: r.body().byteStream());
	}
}
