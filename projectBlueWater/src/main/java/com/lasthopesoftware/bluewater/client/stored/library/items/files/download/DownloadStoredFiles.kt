package com.lasthopesoftware.bluewater.client.stored.library.items.files.download;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.InputStream;

public interface DownloadStoredFiles {
	Promise<InputStream> promiseDownload(LibraryId libraryId, StoredFile storedFile);
}
