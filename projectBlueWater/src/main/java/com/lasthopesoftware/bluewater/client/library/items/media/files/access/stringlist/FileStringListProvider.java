package com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.namehillsoftware.handoff.promises.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileStringListProvider {
	private static final Logger logger = LoggerFactory.getLogger(FileStringListProvider.class);
	private final IConnectionProvider connectionProvider;

	public FileStringListProvider(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	public Promise<String> promiseFileStringList(FileListParameters.Options option, String... params) {
		return connectionProvider
			.call(FileListParameters.Helpers.processParams(option, params))
			.then(response -> response.body().string());
	}
}
