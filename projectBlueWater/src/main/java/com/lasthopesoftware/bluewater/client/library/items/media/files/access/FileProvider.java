package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.providers.AbstractProvider;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

public class FileProvider {
	private static final Logger logger = LoggerFactory.getLogger(FileProvider.class);
	private final FileListParameters.Options option;
	private final String[] parameters;
	private final IConnectionProvider connectionProvider;

	public FileProvider(IConnectionProvider connectionProvider, IFileListParameterProvider item) {
		this(connectionProvider, item, FileListParameters.Options.None);
	}

	private FileProvider(IConnectionProvider connectionProvider, IFileListParameterProvider item, FileListParameters.Options option) {
		this(connectionProvider, option, item.getFileListParameters());
	}

	FileProvider(IConnectionProvider connectionProvider, FileListParameters.Options option, String... parameters) {
		this.connectionProvider = connectionProvider;
		this.option = option;
		this.parameters = parameters;
	}

	public Promise<List<ServiceFile>> promiseFiles() {
		return new QueuedPromise<>(() -> {
			final String[] allConnectionParams = FileListParameters.Helpers.processParams(option, parameters);
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
		}, AbstractProvider.providerExecutor).then(FileStringListUtilities::promiseParsedFileStringList);
	}
}
