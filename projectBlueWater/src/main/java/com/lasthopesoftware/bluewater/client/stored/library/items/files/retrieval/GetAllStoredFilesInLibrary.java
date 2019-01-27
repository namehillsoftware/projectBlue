package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface GetAllStoredFilesInLibrary {
	Promise<Collection<StoredFile>> promiseAllStoredFiles(Library library);
}
