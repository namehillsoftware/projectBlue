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

final class PruneFilesTask implements PromisedResponse<Collection<StoredFile>, Collection<Void>> {
	private static final ExecutorService pruneFilesExecutor = Executors.newSingleThreadExecutor();

	private final Lazy<Set<Integer>> lazyServiceIdsToKeep;
	private final StoredFileAccess storedFileAccess;

	PruneFilesTask(StoredFileAccess storedFileAccess, Collection<ServiceFile> serviceFilesToKeep) {
		this.lazyServiceIdsToKeep = new Lazy<>(() -> Stream.of(serviceFilesToKeep).map(ServiceFile::getKey).collect(Collectors.toSet()));
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public Promise<Collection<Void>> promiseResponse(Collection<StoredFile> allStoredFiles) {
		final Stream<Promise<Void>> pruneFilesPromises =
			Stream.of(allStoredFiles)
				.map(storedFile -> new QueuedPromise<Void>(() -> {
					final String filePath = storedFile.getPath();
					// It doesn't make sense to create a stored serviceFile without a serviceFile path
					if (filePath == null) {
						storedFileAccess.deleteStoredFile(storedFile);
						return null;
					}

					final File systemFile = new File(filePath);

					// Remove files that are marked as downloaded but the serviceFile doesn't actually exist
					if (storedFile.isDownloadComplete() && !systemFile.exists()) {
						storedFileAccess.deleteStoredFile(storedFile);
						return null;
					}

					if (!storedFile.isOwner()) return null;
					if (lazyServiceIdsToKeep.getObject().contains(storedFile.getServiceId())) return null;

					storedFileAccess.deleteStoredFile(storedFile);

					if (!systemFile.delete()) return null;

					File directoryToDelete = systemFile.getParentFile();
					while (directoryToDelete != null) {
						if (directoryToDelete.list().length == 0) {
							if (!directoryToDelete.delete()) return null;
						}
						directoryToDelete = directoryToDelete.getParentFile();
					}
					return null;
				}, pruneFilesExecutor));
		
		return Promise.whenAll(pruneFilesPromises.toList());
	}
}
