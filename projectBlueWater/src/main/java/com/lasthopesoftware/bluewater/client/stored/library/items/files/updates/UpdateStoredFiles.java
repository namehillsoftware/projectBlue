package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface UpdateStoredFiles {
	Promise<StoredFile> promiseStoredFileUpdate(LibraryId libraryId, ServiceFile serviceFile);
}
