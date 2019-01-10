package com.lasthopesoftware.bluewater.client.stored.library.items.files;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface CheckForAnyStoredFiles {
	Promise<Boolean> promiseIsAnyStoredFiles(Library library);
}
