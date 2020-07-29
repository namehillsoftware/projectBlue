package com.lasthopesoftware.bluewater.client.stored.library.items.files;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;
import java.util.Set;

import kotlin.Unit;

public interface IStoredFileAccess {
	Promise<StoredFile> getStoredFile(int storedFileId);

	Promise<StoredFile> getStoredFile(Library library, ServiceFile serviceServiceFile);

	Promise<List<StoredFile>> getDownloadingStoredFiles();

	Promise<StoredFile> markStoredFileAsDownloaded(StoredFile storedFile);

	Promise<Void> addMediaFile(Library library, ServiceFile serviceFile, int mediaFileId, String filePath);

	Promise<Unit> pruneStoredFiles(LibraryId libraryId, Set<ServiceFile> serviceFilesToKeep);
}
