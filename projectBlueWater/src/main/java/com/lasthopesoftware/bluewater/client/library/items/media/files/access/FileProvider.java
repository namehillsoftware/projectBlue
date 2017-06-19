package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.promises.Promise;

import java.util.List;

public class FileProvider {
	private final FileStringListProvider fileStringListProvider;

	public FileProvider(FileStringListProvider fileStringListProvider) {
		this.fileStringListProvider = fileStringListProvider;
	}

	public Promise<List<ServiceFile>> promiseFiles(IFileListParameterProvider fileListParameterProvider) {
		return
			fileStringListProvider
				.promiseFileStringList(fileListParameterProvider)
				.then(FileStringListUtilities::promiseParsedFileStringList);
	}
}
