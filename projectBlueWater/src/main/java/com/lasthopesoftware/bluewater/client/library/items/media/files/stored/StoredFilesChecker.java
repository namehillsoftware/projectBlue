package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public class StoredFilesChecker implements CheckForAnyStoredFiles {

	private final GetAllStoredFilesInLibrary getAllStoredFilesInLibrary;

	public StoredFilesChecker(GetAllStoredFilesInLibrary getAllStoredFilesInLibrary) {
		this.getAllStoredFilesInLibrary = getAllStoredFilesInLibrary;
	}

	@Override
	public Promise<Boolean> promiseIsAnyStoredFiles(Library library) {
		return getAllStoredFilesInLibrary.promiseAllStoredFiles(library)
			.then(storedFiles -> !storedFiles.isEmpty());
	}
}
