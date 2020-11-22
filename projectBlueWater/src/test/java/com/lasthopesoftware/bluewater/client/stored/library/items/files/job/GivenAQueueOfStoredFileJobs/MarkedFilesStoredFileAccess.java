package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.GivenAQueueOfStoredFileJobs;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import kotlin.Unit;

public class MarkedFilesStoredFileAccess implements IStoredFileAccess {
	public final List<StoredFile> storedFilesMarkedAsDownloaded = new ArrayList<>();

	@NotNull
	@Override
	public Promise<StoredFile> getStoredFile(int storedFileId) {
		return Promise.empty();
	}

	@NotNull
	@Override
	public Promise<StoredFile> getStoredFile(@NotNull Library library, @NotNull ServiceFile serviceServiceFile) {
		return Promise.empty();
	}

	@NotNull
	@Override
	public Promise<List<StoredFile>> getDownloadingStoredFiles() {
		return new Promise<>(Collections.emptyList());
	}

	@NotNull
	@Override
	public Promise<StoredFile> markStoredFileAsDownloaded(StoredFile storedFile) {
		storedFilesMarkedAsDownloaded.add(storedFile);
		return new Promise<>(storedFile);
	}

	@NotNull
	@Override
	public Promise<Unit> addMediaFile(@NotNull Library library, @NotNull ServiceFile serviceFile, int mediaFileId, String filePath) {
		return new Promise<>(Unit.INSTANCE);
	}

	@NotNull
	@Override
	public Promise<Unit> pruneStoredFiles(@NotNull LibraryId libraryId, @NotNull Set<ServiceFile> serviceFilesToKeep) {
		return new Promise<>(Unit.INSTANCE);
	}
}
