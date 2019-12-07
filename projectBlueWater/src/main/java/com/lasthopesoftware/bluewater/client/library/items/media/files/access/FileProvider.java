package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

import java.util.Collection;

public final class FileProvider implements IFileProvider, PromisedResponse<String, Collection<ServiceFile>> {
	private final FileStringListProvider fileStringListProvider;

	public FileProvider(FileStringListProvider fileStringListProvider) {
		this.fileStringListProvider = fileStringListProvider;
	}

	@Override
	public Promise<Collection<ServiceFile>> promiseFiles(FileListParameters.Options option, String... params) {
		return
			fileStringListProvider
				.promiseFileStringList(option, params)
				.eventually(this);
	}

	@Override
	public Promise<Collection<ServiceFile>> promiseResponse(String stringList) {
		return FileStringListUtilities.promiseParsedFileStringList(stringList);
	}
}
