package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface GetAllStoredFilesInLibrary {
	Promise<Collection<StoredFile>> promiseAllStoredFiles(LibraryId libraryId);
}
