package com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface GetStoredFiles {
	Promise<StoredFile> promiseStoredFile(LibraryId libraryId, ServiceFile serviceFile);
}
