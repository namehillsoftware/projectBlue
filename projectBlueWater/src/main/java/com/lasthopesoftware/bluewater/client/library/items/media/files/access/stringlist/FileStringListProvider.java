package com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.providers.AbstractProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public final class FileStringListProvider {
	private static final Logger logger = LoggerFactory.getLogger(FileStringListProvider.class);
	private final IConnectionProvider connectionProvider;

	public FileStringListProvider(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	public Promise<String> promiseFileStringList(FileListParameters.Options option, String... params) {
		return new QueuedPromise<>(() -> {
			final String[] allConnectionParams = FileListParameters.Helpers.processParams(option, params);
			final HttpURLConnection connection = connectionProvider.getConnection(allConnectionParams);
			try {
				try (final InputStream is = connection.getInputStream()) {
					return IOUtils.toString(is);
				}
			} catch (IOException e) {
				logger.warn("There was an error getting the serviceFile list", e);
				throw e;
			} finally {
				connection.disconnect();
			}
		}, AbstractProvider.providerExecutor);
	}
}
