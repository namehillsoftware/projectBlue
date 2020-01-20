package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAQueueOfStoredFileJobs;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MarkedFilesStoredFileAccess implements IStoredFileAccess {
	public final List<StoredFile> storedFilesMarkedAsDownloaded = new ArrayList<>();

	@Override
	public Promise<StoredFile> getStoredFile(int storedFileId) {
		return Promise.empty();
	}

	@Override
	public Promise<StoredFile> getStoredFile(Library library, ServiceFile serviceServiceFile) {
		return Promise.empty();
	}

	@Override
	public Promise<List<StoredFile>> getDownloadingStoredFiles() {
		return new Promise<>(Collections.emptyList());
	}

	@Override
	public Promise<StoredFile> markStoredFileAsDownloaded(StoredFile storedFile) {
		storedFilesMarkedAsDownloaded.add(storedFile);
		return new Promise<>(storedFile);
	}

	@Override
	public Promise<Void> addMediaFile(Library library, ServiceFile serviceFile, int mediaFileId, String filePath) {
		return Promise.empty();
	}

	@Override
	public Promise<Void> pruneStoredFiles(LibraryId libraryId, Set<ServiceFile> serviceFilesToKeep) {
		return Promise.empty();
	}
}
