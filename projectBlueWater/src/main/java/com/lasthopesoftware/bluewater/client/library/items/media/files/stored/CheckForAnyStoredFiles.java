package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface CheckForAnyStoredFiles {
	Promise<Boolean> promiseIsAnyStoredFiles(Library library);
}
