package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IStoredFileAccess {
	Promise<StoredFile> getStoredFile(int storedFileId);

	Promise<StoredFile> getStoredFile(Library library, ServiceFile serviceServiceFile);

	Promise<List<StoredFile>> getDownloadingStoredFiles();

	Promise<StoredFile> markStoredFileAsDownloaded(StoredFile storedFile);

	Promise<Void> addMediaFile(Library library, ServiceFile serviceFile, int mediaFileId, String filePath);

	Promise<StoredFile> promiseStoredFileUpsert(Library library, ServiceFile serviceFile);

	Promise<Collection<Void>> pruneStoredFiles(Library library, Set<ServiceFile> serviceFilesToKeep);
}
