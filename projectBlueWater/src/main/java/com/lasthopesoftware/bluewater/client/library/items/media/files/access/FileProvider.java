package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import java.util.List;

public final class FileProvider implements CarelessOneParameterFunction<String, Promise<List<ServiceFile>>> {
	private final FileStringListProvider fileStringListProvider;

	public FileProvider(FileStringListProvider fileStringListProvider) {
		this.fileStringListProvider = fileStringListProvider;
	}

	public Promise<List<ServiceFile>> promiseFiles(FileListParameters.Options option, String... params) {
		return
			fileStringListProvider
				.promiseFileStringList(option, params)
				.then(this);
	}

	@Override
	public Promise<List<ServiceFile>> resultFrom(String stringList) throws Throwable {
		return FileStringListUtilities.promiseParsedFileStringList(stringList);
	}
}
