package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.lazyj.Lazy;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class PruneFilesTask implements PromisedResponse<Collection<StoredFile>, Void> {
	private static final ExecutorService pruneFilesExecutor = Executors.newSingleThreadExecutor();

	private final Lazy<Set<Integer>> lazyServiceIdsToKeep;
	private final StoredFileAccess storedFileAccess;

	PruneFilesTask(StoredFileAccess storedFileAccess, Collection<ServiceFile> serviceFilesToKeep) {
		this.lazyServiceIdsToKeep = new Lazy<>(() -> Stream.of(serviceFilesToKeep).map(ServiceFile::getKey).collect(Collectors.toSet()));
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public Promise<Void> promiseResponse(Collection<StoredFile> allStoredFiles) {
		return new QueuedPromise<>(() -> {
			for (StoredFile storedFile : allStoredFiles) {
				final String filePath = storedFile.getPath();
				// It doesn't make sense to create a stored serviceFile without a serviceFile path
				if (filePath == null) {
					storedFileAccess.deleteStoredFile(storedFile);
					continue;
				}

				final File systemFile = new File(filePath);

				// Remove files that are marked as downloaded but the serviceFile doesn't actually exist
				if (storedFile.isDownloadComplete() && !systemFile.exists()) {
					storedFileAccess.deleteStoredFile(storedFile);
					continue;
				}

				if (!storedFile.isOwner()) continue;
				if (lazyServiceIdsToKeep.getObject().contains(storedFile.getServiceId())) continue;

				storedFileAccess.deleteStoredFile(storedFile);

				if (!systemFile.delete()) continue;

				File directoryToDelete = systemFile.getParentFile();
				while (directoryToDelete != null) {
					final String[] childList = directoryToDelete.list();
					if (childList != null && childList.length > 0)
						break;

					if (!directoryToDelete.delete()) break;
					directoryToDelete = directoryToDelete.getParentFile();
				}
			}

			return null;
		}, pruneFilesExecutor);
	}
}
