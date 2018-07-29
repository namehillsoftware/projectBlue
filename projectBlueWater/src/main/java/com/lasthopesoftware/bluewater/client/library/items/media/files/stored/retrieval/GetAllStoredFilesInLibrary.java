package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.retrieval;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public interface GetAllStoredFilesInLibrary {
	Promise<Collection<StoredFile>> promiseAllStoredFiles(Library library);
}
