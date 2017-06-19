package com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.providers.AbstractProvider;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class FileStringListProvider {
	private static final Logger logger = LoggerFactory.getLogger(FileStringListProvider.class);
	private final FileListParameters.Options option;
	private final IConnectionProvider connectionProvider;

	public FileStringListProvider(ConnectionProvider connectionProvider) {
		this(connectionProvider, FileListParameters.Options.None);
	}

	public FileStringListProvider(ConnectionProvider connectionProvider, FileListParameters.Options option) {
		this.connectionProvider = connectionProvider;
		this.option = option;
	}

	public Promise<String> promiseFileStringList(IFileListParameterProvider fileListParameterProvider) {
		return new QueuedPromise<>(() -> {
			final String[] allConnectionParams = FileListParameters.Helpers.processParams(option, fileListParameterProvider.getFileListParameters());
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
