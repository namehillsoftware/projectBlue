package com.lasthopesoftware.bluewater.client.library.items.media.files.cached;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.IDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.MessageWriter;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Flush a given cache until it reaches the given target size
 * @author david
 *
 */
public class CacheFlusherTask implements MessageWriter<Void> {

	private final static Logger logger = LoggerFactory.getLogger(CacheFlusherTask.class);

	private final Context context;
	private final IDiskCacheDirectoryProvider diskCacheDirectory;
	private final IDiskFileCacheConfiguration diskFileCacheConfiguration;
	private final long targetSize;

	public static Promise<?> promisedCacheFlushing(final Context context, final IDiskCacheDirectoryProvider diskCacheDirectory, final IDiskFileCacheConfiguration diskFileCacheConfiguration, final long targetSize) {
		return new QueuedPromise<>(new CacheFlusherTask(context, diskCacheDirectory, diskFileCacheConfiguration, targetSize), RepositoryAccessHelper.databaseExecutor());
	}

	/*
	 * Flush a given cache until it reaches the given target size
	 */
	private CacheFlusherTask(final Context context, final IDiskCacheDirectoryProvider diskCacheDirectory, final IDiskFileCacheConfiguration diskFileCacheConfiguration, final long targetSize) {
		this.context = context;
		this.diskCacheDirectory = diskCacheDirectory;
		this.diskFileCacheConfiguration = diskFileCacheConfiguration;
		this.targetSize = targetSize;
	}

	@Override
	public Void prepareMessage() {
		flushCache();
		return null;
	}

	private void flushCache() {
		try (final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			if (getCachedFileSizeFromDatabase(repositoryAccessHelper) <= targetSize) return;

			do {
				final CachedFile cachedFile = getOldestCachedFile(repositoryAccessHelper);
				if (cachedFile != null)
					deleteCachedFile(repositoryAccessHelper, cachedFile);
			} while (getCachedFileSizeFromDatabase(repositoryAccessHelper) > targetSize);

			// Remove any files in the cache dir but not in the database
			final File cacheDir = diskCacheDirectory.getDiskCacheDirectory(diskFileCacheConfiguration);

			if (cacheDir == null || !cacheDir.exists()) return;

			final File[] filesInCacheDir = cacheDir.listFiles();

			// If the # of files in the cache dir is equal to the database size, then
			// hypothetically (and good enough for our purposes), they are in sync and we don't need
			// to do additional processing
			if (filesInCacheDir == null || filesInCacheDir.length == getCachedFileCount(repositoryAccessHelper))
				return;

			// Remove all files that aren't tracked in the database
			for (final File fileInCacheDir : filesInCacheDir) {
				try {
					if (getCachedFileByFilename(repositoryAccessHelper, fileInCacheDir.getCanonicalPath()) != null)
						continue;
				} catch (IOException e) {
					logger.warn("Issue getting canonical serviceFile path.");
				}

				if (!fileInCacheDir.delete())
					logger.warn("The cached file `" + fileInCacheDir.getPath() + "` could not be deleted.");
			}
		}
	}

	private long getCachedFileSizeFromDatabase(final RepositoryAccessHelper repositoryAccessHelper) {
		return repositoryAccessHelper
				.mapSql("SELECT SUM(" + CachedFile.FILE_SIZE + ") FROM " + CachedFile.tableName + " WHERE " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME)
				.addParameter(CachedFile.CACHE_NAME, diskFileCacheConfiguration.getCacheName())
				.execute();
	}
	
//	private final long getCacheSizeBetweenTimes(final Dao<CachedFile, Integer> cachedFileAccess, final long startTime, final long endTime) {
//		try {
//			
//			final PreparedQuery<CachedFile> preparedQuery =
//					cachedFileAccess.queryBuilder()
//						.selectRaw("SUM(" + CachedFile.FILE_SIZE + ")")
//						.where()
//						.eq(CachedFile.CACHE_NAME, new SelectArg())
//						.and()
//						.between(CachedFile.CREATED_TIME, new SelectArg(), new SelectArg())
//						.prepare();
//			
//			return cachedFileAccess.queryRawValue(preparedQuery.getStatement(), cacheName, String.valueOf(startTime), String.valueOf(endTime));
//		} catch (SQLException e) {
//			logger.excuse("Error getting serviceFile size", e);
//			return -1;
//		}
//	}
	
	private CachedFile getOldestCachedFile(final RepositoryAccessHelper repositoryAccessHelper) {
		return repositoryAccessHelper
				.mapSql("SELECT * FROM " + CachedFile.tableName + " WHERE " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME + " ORDER BY " + CachedFile.LAST_ACCESSED_TIME + " ASC")
				.addParameter(CachedFile.CACHE_NAME, diskFileCacheConfiguration.getCacheName())
				.fetchFirst(CachedFile.class);
	}
	
	private long getCachedFileCount(final RepositoryAccessHelper repositoryAccessHelper) {
		return repositoryAccessHelper
				.mapSql("SELECT COUNT(*) FROM " + CachedFile.tableName + " WHERE " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME)
				.addParameter(CachedFile.CACHE_NAME, diskFileCacheConfiguration.getCacheName())
				.execute();
	}
	
	private static CachedFile getCachedFileByFilename(final RepositoryAccessHelper repositoryAccessHelper, final String fileName) {

		return repositoryAccessHelper
				.mapSql("SELECT * FROM " + CachedFile.tableName + " WHERE " + CachedFile.FILE_NAME + " = @" + CachedFile.FILE_NAME)
				.addParameter(CachedFile.FILE_NAME, fileName)
				.fetchFirst(CachedFile.class);

	}
	
	private static boolean deleteCachedFile(final RepositoryAccessHelper repositoryAccessHelper, final CachedFile cachedFile) {
		final File fileToDelete = new File(cachedFile.getFileName());

		return (fileToDelete.exists() && fileToDelete.delete())
			& repositoryAccessHelper
				.mapSql("DELETE FROM " + CachedFile.tableName + " WHERE id = @id")
				.addParameter("id", cachedFile.getId())
				.execute() > 0;
	}
}
