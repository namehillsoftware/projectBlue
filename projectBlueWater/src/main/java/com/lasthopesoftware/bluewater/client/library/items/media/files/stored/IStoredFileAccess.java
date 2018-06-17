package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IStoredFileAccess {
	Promise<StoredFile> getStoredFile(int storedFileId);

	Promise<StoredFile> getStoredFile(ServiceFile serviceServiceFile);

	Promise<List<StoredFile>> getDownloadingStoredFiles();

	void markStoredFileAsDownloaded(StoredFile storedFile);

	Promise<Void> addMediaFile(ServiceFile serviceFile, int mediaFileId, String filePath);

	Promise<StoredFile> createOrUpdateFile(ServiceFile serviceFile);

	Promise<Collection<Void>> pruneStoredFiles(Set<ServiceFile> serviceFilesToKeep);
}
