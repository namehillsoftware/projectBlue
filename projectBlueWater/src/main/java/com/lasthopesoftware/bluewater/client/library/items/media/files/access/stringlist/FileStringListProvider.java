package com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.namehillsoftware.handoff.promises.Promise;

public final class FileStringListProvider {
	private final IConnectionProvider connectionProvider;

	public FileStringListProvider(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	public Promise<String> promiseFileStringList(FileListParameters.Options option, String... params) {
		return connectionProvider
			.promiseResponse(FileListParameters.Helpers.processParams(option, params))
			.then(response -> response.body().string());
	}
}
