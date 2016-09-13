package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.content.Context;
import android.database.SQLException;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.vedsoft.fluent.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 1/19/16.
 */
public final class PruneFilesTask extends FluentTask<Void, Void, Void> {
	private static final Logger logger = LoggerFactory.getLogger(PruneFilesTask.class);
	private static final ExecutorService pruneFilesExecutor = Executors.newSingleThreadExecutor();

	private final Set<Integer> serviceIdsToKeep;
	private final StoredFileAccess storedFileAccess;

	public PruneFilesTask(Context context, Library library, Set<Integer> serviceIdsToKeep) {
		super(pruneFilesExecutor);

		this.serviceIdsToKeep = serviceIdsToKeep;
		this.storedFileAccess = new StoredFileAccess(context, library);
	}

	@Override
	protected Void executeInBackground(Void[] voidParams) {
		try {
			final List<StoredFile> allStoredFiles = storedFileAccess.getAllStoredFilesInLibrary();

			for (StoredFile storedFile : allStoredFiles) {
				final String filePath = storedFile.getPath();
				// It doesn't make sense to create a stored file without a file path
				if (filePath == null) {
					storedFileAccess.deleteStoredFile(storedFile);
					continue;
				}

				final File systemFile = new File(filePath);

				// Remove files that are marked as downloaded but the file doesn't actually exist
				if (storedFile.isDownloadComplete() && !systemFile.exists()) {
					storedFileAccess.deleteStoredFile(storedFile);
					continue;
				}

				if (!storedFile.isOwner()) continue;
				if (serviceIdsToKeep.contains(storedFile.getServiceId())) continue;

				storedFileAccess.deleteStoredFile(storedFile);
				systemFile.delete();
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			logger.error("There was an error getting the stored files", e);
		}

		return null;
	}
}
