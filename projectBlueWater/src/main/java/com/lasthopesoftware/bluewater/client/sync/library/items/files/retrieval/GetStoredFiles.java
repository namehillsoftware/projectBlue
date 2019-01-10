package com.lasthopesoftware.bluewater.client.sync.library.items.files.retrieval;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.sync.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface GetStoredFiles {
	Promise<StoredFile> promiseStoredFile(Library library, ServiceFile serviceFile);

	Promise<StoredFile> promiseStoredFile(int storedFileId);
}
