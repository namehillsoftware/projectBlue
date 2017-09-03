package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.messenger.promises.Promise;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IStoredFileAccess {
	Promise<StoredFile> getStoredFile(int storedFileId);

	Promise<StoredFile> getStoredFile(ServiceFile serviceServiceFile);

	Promise<List<StoredFile>> getDownloadingStoredFiles();

	void markStoredFileAsDownloaded(StoredFile storedFile);

	void addMediaFile(ServiceFile serviceFile, int mediaFileId, String filePath);

	Promise<StoredFile> createOrUpdateFile(IConnectionProvider connectionProvider, ServiceFile serviceFile);

	Promise<Collection<Void>> pruneStoredFiles(Set<ServiceFile> serviceFilesToKeep);
}
