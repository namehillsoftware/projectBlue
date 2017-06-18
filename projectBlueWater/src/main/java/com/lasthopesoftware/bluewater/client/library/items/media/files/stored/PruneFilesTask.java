package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.content.Context;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class PruneFilesTask implements CarelessOneParameterFunction<Collection<StoredFile>, Promise<Collection<Void>>> {
	private static final Logger logger = LoggerFactory.getLogger(PruneFilesTask.class);
	private static final ExecutorService pruneFilesExecutor = Executors.newSingleThreadExecutor();

	private final Set<Integer> serviceIdsToKeep;
	private final StoredFileAccess storedFileAccess;

	PruneFilesTask(Context context, Library library, Set<Integer> serviceIdsToKeep) {
		this.serviceIdsToKeep = serviceIdsToKeep;
		this.storedFileAccess = new StoredFileAccess(context, library);
	}

	@Override
	public Promise<Collection<Void>> resultFrom(Collection<StoredFile> allStoredFiles) {
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
					if (serviceIdsToKeep.contains(storedFile.getServiceId())) return null;

					storedFileAccess.deleteStoredFile(storedFile);
					systemFile.delete();
					return null;
				}, pruneFilesExecutor));

		return Promise.whenAll(pruneFilesPromises.collect(Collectors.toList()));
	}
}
