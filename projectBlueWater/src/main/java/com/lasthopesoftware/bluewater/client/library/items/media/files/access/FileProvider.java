package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class FileProvider implements IFileProvider, PromisedResponse<String, Collection<ServiceFile>>, ImmediateResponse<Collection<ServiceFile>, List<ServiceFile>> {
	private final FileStringListProvider fileStringListProvider;

	public FileProvider(FileStringListProvider fileStringListProvider) {
		this.fileStringListProvider = fileStringListProvider;
	}

	@Override
	public Promise<List<ServiceFile>> promiseFiles(FileListParameters.Options option, String... params) {
		return
			fileStringListProvider
				.promiseFileStringList(option, params)
				.eventually(this)
				.then(this);
	}

	@Override
	public Promise<List<ServiceFile>> promiseFiles(LibraryId libraryId, FileListParameters.Options option, String... params) {
		return
			fileStringListProvider
				.promiseFileStringList(libraryId, option, params)
				.eventually(this)
				.then(this);
	}

	@Override
	public Promise<Collection<ServiceFile>> promiseResponse(String stringList) {
		return FileStringListUtilities.promiseParsedFileStringList(stringList);
	}

	@Override
	public List<ServiceFile> respond(Collection<ServiceFile> serviceFiles) {
		return serviceFiles instanceof List
			? (List<ServiceFile>)serviceFiles
			: new ArrayList<>(serviceFiles);
	}
}
