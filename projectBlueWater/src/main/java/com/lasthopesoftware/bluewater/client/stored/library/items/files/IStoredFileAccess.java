package com.lasthopesoftware.bluewater.client.stored.library.items.files;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;
import java.util.Set;

public interface IStoredFileAccess {
	Promise<StoredFile> getStoredFile(int storedFileId);

	Promise<StoredFile> getStoredFile(Library library, ServiceFile serviceServiceFile);

	Promise<List<StoredFile>> getDownloadingStoredFiles();

	Promise<StoredFile> markStoredFileAsDownloaded(StoredFile storedFile);

	Promise<Void> addMediaFile(Library library, ServiceFile serviceFile, int mediaFileId, String filePath);

	Promise<Void> pruneStoredFiles(LibraryId libraryId, Set<ServiceFile> serviceFilesToKeep);
}
