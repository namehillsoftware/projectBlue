package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.content.Context;
import android.database.SQLException;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.vedsoft.fluent.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Created by david on 1/19/16.
 */
public final class PruneFilesTask extends FluentTask<Void, Void, Void> {
	private static final Logger logger = LoggerFactory.getLogger(PruneFilesTask.class);

	private final Context context;
	private final int libraryId;
	private final Set<Integer> serviceIdsToKeep;

	public PruneFilesTask(Context context, int libraryId, Set<Integer> serviceIdsToKeep) {
		this.context = context;
		this.libraryId = libraryId;
		this.serviceIdsToKeep = serviceIdsToKeep;
	}

	@Override
	protected Void executeInBackground(Void[] voidParams) {
		final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
		try {
			final List<StoredFile> allStoredFilesQuery =
					repositoryAccessHelper
							.mapSql("SELECT * FROM " + StoredFileEntityInformation.tableName)
							.fetch(StoredFile.class);

			for (StoredFile storedFile : allStoredFilesQuery) {
				final String filePath = storedFile.getPath();
				// It doesn't make sense to create a stored file without a file path
				if (filePath == null) {
					deleteStoredFile(repositoryAccessHelper, storedFile);
					continue;
				}

				final File systemFile = new File(filePath);

				// Remove files that are marked as downloaded but the file doesn't actually exist
				if (storedFile.isDownloadComplete() && !systemFile.exists()) {
					deleteStoredFile(repositoryAccessHelper, storedFile);
					continue;
				}

				if (!storedFile.isOwner()) continue;
				if (storedFile.getLibraryId() != libraryId) continue;
				if (serviceIdsToKeep.contains(storedFile.getServiceId())) continue;

				deleteStoredFile(repositoryAccessHelper, storedFile);
				systemFile.delete();
			}
		} catch (SQLException e) {
			logger.error("There was an error getting the stored files", e);
		} finally {
			repositoryAccessHelper.close();
		}

		return null;
	}

	private static void deleteStoredFile(RepositoryAccessHelper repositoryAccessHelper, final StoredFile storedFile) {
		try {
			repositoryAccessHelper
					.mapSql("DELETE FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
					.addParameter("id", storedFile.getId())
					.execute();
		} catch (SQLException e) {
			logger.error("There was an error deleting file " + storedFile.getId(), e);
		}
	}
}
