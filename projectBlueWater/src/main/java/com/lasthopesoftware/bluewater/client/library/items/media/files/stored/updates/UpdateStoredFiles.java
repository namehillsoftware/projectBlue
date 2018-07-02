package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.updates;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface UpdateStoredFiles {
	Promise<StoredFile> promiseStoredFileUpdate(Library library, ServiceFile serviceFile);
}
