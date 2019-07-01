package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence;

import android.content.Context;
import android.database.SQLException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class DiskFileAccessTimeUpdater implements IDiskFileAccessTimeUpdater {
	private final static Logger logger = LoggerFactory.getLogger(DiskFileAccessTimeUpdater.class);

	private final Context context;

	public DiskFileAccessTimeUpdater(Context context) {
		this.context = context;
	}

	@Override
	public Promise<CachedFile> promiseFileAccessedUpdate(CachedFile cachedFile) {
		return new QueuedPromise<>(() -> {
			doFileAccessedUpdate(cachedFile.getId());
			return cachedFile;
		}, RepositoryAccessHelper.databaseExecutor);
	}

	private void doFileAccessedUpdate(final long cachedFileId) {
		final long updateTime = System.currentTimeMillis();

		logger.info("Updating accessed time on cached file with ID " + cachedFileId + " to " + new Date(updateTime));

		try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
				repositoryAccessHelper
					.mapSql("UPDATE " + CachedFile.tableName + " SET " + CachedFile.LAST_ACCESSED_TIME + " = @" + CachedFile.LAST_ACCESSED_TIME + " WHERE id = @id")
					.addParameter(CachedFile.LAST_ACCESSED_TIME, updateTime)
					.addParameter("id", cachedFileId)
					.execute();

				closeableTransaction.setTransactionSuccessful();
			} catch (SQLException sqlException) {
				logger.error("There was an error trying to update the cached file with ID " + cachedFileId, sqlException);
				throw sqlException;
			}
		}
	}
}
