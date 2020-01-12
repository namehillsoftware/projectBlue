package com.lasthopesoftware.bluewater.client.stored.library.items.files;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

public class StoredFilesChecker implements CheckForAnyStoredFiles {

	private final CountStoredFiles countStoredFiles;

	public StoredFilesChecker(CountStoredFiles countStoredFiles) {
		this.countStoredFiles = countStoredFiles;
	}

	@Override
	public Promise<Boolean> promiseIsAnyStoredFiles(LibraryId libraryId) {
		return countStoredFiles.promiseStoredFilesCount(libraryId)
			.then(count -> count > 0);
	}
}
