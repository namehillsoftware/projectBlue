package com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access;

import android.content.Context;
import android.database.SQLException;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.repository.CloseableNonExclusiveTransaction;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedFilesProvider implements ICachedFilesProvider {
	private final static Logger logger = LoggerFactory.getLogger(DiskFileCache.class);

	private static final String cachedFileFilter =
		" WHERE " + CachedFile.LIBRARY_ID + " = @" + CachedFile.LIBRARY_ID +
			" AND " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME +
			" AND " + CachedFile.UNIQUE_KEY + " = @" + CachedFile.UNIQUE_KEY;

	private final Context context;
	private final IDiskFileCacheConfiguration diskFileCacheConfiguration;

	public CachedFilesProvider(Context context, IDiskFileCacheConfiguration diskFileCacheConfiguration) {
		this.context = context;
		this.diskFileCacheConfiguration = diskFileCacheConfiguration;
	}

	@Override
	public Promise<CachedFile> promiseCachedFile(String uniqueKey) {
		return new QueuedPromise<>(() -> getCachedFile(uniqueKey), RepositoryAccessHelper.databaseExecutor());
	}

	private CachedFile getCachedFile(final String uniqueKey) {
		try (final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			try (final CloseableNonExclusiveTransaction closeableNonExclusiveTransaction = repositoryAccessHelper.beginNonExclusiveTransaction()) {
				final CachedFile cachedFile = repositoryAccessHelper
					.mapSql("SELECT * FROM " + CachedFile.tableName + cachedFileFilter)
					.addParameter(CachedFile.LIBRARY_ID, diskFileCacheConfiguration.getLibrary().getId())
					.addParameter(CachedFile.CACHE_NAME, diskFileCacheConfiguration.getCacheName())
					.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
					.fetchFirst(CachedFile.class);

				closeableNonExclusiveTransaction.setTransactionSuccessful();
				return cachedFile;
			} catch (SQLException sqlException) {
				logger.error("There was an error getting the cached file with unique key " + uniqueKey, sqlException);
				return null;
			}
		}
	}
}
