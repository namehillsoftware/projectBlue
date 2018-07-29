package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.retrieval;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface GetStoredFiles {
	Promise<StoredFile> promiseStoredFile(Library library, ServiceFile serviceFile);

	Promise<StoredFile> promiseStoredFile(int storedFileId);
}
