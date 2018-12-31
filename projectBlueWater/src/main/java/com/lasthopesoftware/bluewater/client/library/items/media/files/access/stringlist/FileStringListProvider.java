package com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.ResponseBody;

public final class FileStringListProvider {
	private final IConnectionProvider connectionProvider;

	public FileStringListProvider(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	public Promise<String> promiseFileStringList(FileListParameters.Options option, String... params) {
		return connectionProvider
			.promiseResponse(FileListParameters.Helpers.processParams(option, params))
			.then(response -> {
				final ResponseBody body = response.body();
				if (body == null) return null;

				try {
					return body.string();
				} finally {
					body.close();
				}
			});
	}
}
