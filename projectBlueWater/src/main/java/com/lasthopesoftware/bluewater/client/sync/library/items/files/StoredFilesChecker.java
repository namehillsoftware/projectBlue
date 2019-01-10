package com.lasthopesoftware.bluewater.client.sync.library.items.files;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public class StoredFilesChecker implements CheckForAnyStoredFiles {

	private final CountStoredFiles countStoredFiles;

	public StoredFilesChecker(CountStoredFiles countStoredFiles) {
		this.countStoredFiles = countStoredFiles;
	}

	@Override
	public Promise<Boolean> promiseIsAnyStoredFiles(Library library) {
		return countStoredFiles.promiseStoredFilesCount(library)
			.then(count -> count > 0);
	}
}
