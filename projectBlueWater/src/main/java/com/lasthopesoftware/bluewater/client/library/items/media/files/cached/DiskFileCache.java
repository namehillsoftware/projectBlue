package com.lasthopesoftware.bluewater.client.library.items.media.files.cached;

import android.content.Context;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Environment;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.repository.CloseableNonExclusiveTransaction;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.vedsoft.lazyj.AbstractSynchronousLazy;
import com.vedsoft.lazyj.ILazy;
import com.vedsoft.lazyj.Lazy;
import com.vedsoft.objective.droid.ObjectiveDroid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLDataException;
import java.util.Date;

public class DiskFileCache {
	
	private final static long msInDay = 86400000L;
	
	private static final String cachedFileFilter =
			" WHERE " + CachedFile.LIBRARY_ID + " = @" + CachedFile.LIBRARY_ID +
			" AND " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME +
			" AND " + CachedFile.UNIQUE_KEY + " = @" + CachedFile.UNIQUE_KEY;

	private static final Lazy<String> cachedFileSqlInsert =
			new Lazy<>(() ->
					InsertBuilder
						.fromTable(CachedFile.tableName)
						.addColumn(CachedFile.CACHE_NAME)
						.addColumn(CachedFile.FILE_NAME)
						.addColumn(CachedFile.FILE_SIZE)
						.addColumn(CachedFile.LIBRARY_ID)
						.addColumn(CachedFile.UNIQUE_KEY)
						.addColumn(CachedFile.CREATED_TIME)
						.addColumn(CachedFile.LAST_ACCESSED_TIME)
						.build());

	private final static Logger logger = LoggerFactory.getLogger(DiskFileCache.class);
	private final Context context;
	private final Library library;
	private final String cacheName;
	private final long maxSize;

	private final long expirationTime;

	private final ILazy<File> lazyDiskCacheDir = new AbstractSynchronousLazy<File>() {
		@Override
		protected File initialize() throws Exception {
			final java.io.File cacheDir = new File(DiskFileCache.getDiskCacheDir(context, cacheName), String.valueOf(library.getId()));
			if (!cacheDir.exists() && !cacheDir.mkdirs()) return null;

			return cacheDir;
		}
	};

	public DiskFileCache(final Context context, final Library library, final String cacheName, final int expirationDays, final long maxSize) {
		this.context = context;
		this.cacheName = cacheName;
		this.maxSize = maxSize;
		this.library = library;
		expirationTime = expirationDays * msInDay;
	}

	public Promise<Void> put(final String uniqueKey, final byte[] fileData) {
		// Just execute this on the thread pool executor as it doesn't write to the database
		final QueuedPromise<Void> putPromise =
			new QueuedPromise<>(() -> {
				final String suffix = ".cache";
				final String uniqueKeyHashCode = String.valueOf(uniqueKey.hashCode());
				final File diskCacheDir = lazyDiskCacheDir.getObject();
				File file = new File(diskCacheDir, uniqueKeyHashCode + suffix);

				if (file.exists()) {
					int collisionNumber = 0;
					do {
						file = new File(diskCacheDir, uniqueKeyHashCode + "-" + collisionNumber++ + suffix);
					} while (file.exists());
				}

				do {
					try {
						try (FileOutputStream fos = new FileOutputStream(file)) {
							fos.write(fileData);
							fos.flush();
						}

						putIntoDatabase(uniqueKey, file);
						return null;
					} catch (IOException e) {
						logger.error("Unable to write to serviceFile!", e);

						// Check if free space is too low and then attempt to free up enough space
						// to store image
						final long freeDiskSpace = getFreeDiskSpace(context);
						if (freeDiskSpace > maxSize) {
							return null;
						}

						CacheFlusherTask.futureCacheFlushing(context, cacheName, freeDiskSpace + file.length()).get();
					}
				} while (getFreeDiskSpace(context) >= file.length());

				return null;
			}, AsyncTask.THREAD_POOL_EXECUTOR);

		putPromise.excuse(e -> {
			if (e instanceof IOException) throw e;

			logger.error("There was an excuse putting the serviceFile with the unique key " + uniqueKey + " into the cache.", e);

			return null;
		});

		return putPromise;
	}

	private void putIntoDatabase(final String uniqueKey, final File file) {
		RepositoryAccessHelper.databaseExecutor.execute(() -> {
			final String canonicalFilePath;
			try {
				canonicalFilePath = file.getCanonicalPath();
			} catch (IOException e) {
				logger.error("There was an excuse getting the canonical path for " + file, e);
				return;
			}

			final CachedFile cachedFile;
			try {
				cachedFile = getCachedFile(uniqueKey);
			} catch (IOException e) {
				logger.error("There was an excuse getting the cached serviceFile with unique key " + uniqueKey, e);
				return;
			}

			if (cachedFile != null) {
				if (!cachedFile.getFileName().equals(canonicalFilePath)) {
					try {
						updateFilePath(cachedFile.getId(), canonicalFilePath);
					} catch (SQLException e) {
						return;
					}
				}

				doFileAccessedUpdate(cachedFile.getId());
				return;
			}

			logger.info("ServiceFile with unique key " + uniqueKey + " doesn't exist. Creating...");
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				final ObjectiveDroid sqlInsertMapper = repositoryAccessHelper.mapSql(cachedFileSqlInsert.getObject());

				sqlInsertMapper.addParameter(CachedFile.FILE_NAME, canonicalFilePath);

				try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
					final long currentTimeMillis = System.currentTimeMillis();
					sqlInsertMapper
							.addParameter(CachedFile.CACHE_NAME, cacheName)
							.addParameter(CachedFile.FILE_SIZE, file.length())
							.addParameter(CachedFile.LIBRARY_ID, library.getId())
							.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
							.addParameter(CachedFile.CREATED_TIME, currentTimeMillis)
							.addParameter(CachedFile.LAST_ACCESSED_TIME, currentTimeMillis)
							.execute();

					closeableTransaction.setTransactionSuccessful();
				} catch (SQLException sqlException) {
					logger.warn("There was an excuse inserting the cached serviceFile with the unique key " + uniqueKey, sqlException);
				}
			} finally {
				CacheFlusherTask.futureCacheFlushing(context, cacheName, maxSize);
			}
		});
	}

	public Promise<File> promiseCachedFile(final String uniqueKey) {
		return new QueuedPromise<>(() -> {
			logger.info("Getting cached serviceFile " + uniqueKey);
			try {
				final CachedFile cachedFile = getCachedFile(uniqueKey);
				if (cachedFile == null)	return null;

				final File returnFile = new File(cachedFile.getFileName());
				logger.info("Checking if " + cachedFile.getFileName() + " exists.");
				if (!returnFile.exists()) {
					logger.warn("Cached serviceFile `" + cachedFile.getFileName() + "` doesn't exist! Removing from database.");
					if (deleteCachedFile(cachedFile.getId()) <= 0)
						throw new SQLDataException("Unable to delete serviceFile with ID " + cachedFile.getId());

					return null;
				}

				// Remove the serviceFile and return null if it's past its expired time
				if (cachedFile.getCreatedTime() < System.currentTimeMillis() - expirationTime) {
					logger.info("Cached serviceFile " + uniqueKey + " expired. Deleting.");
					if (!returnFile.delete())
						throw new IOException("Unable to delete serviceFile " + returnFile.getAbsolutePath());

					if (deleteCachedFile(cachedFile.getId()) <= 0)
						throw new SQLDataException("Unable to delete serviceFile with ID " + cachedFile.getId());

					return null;
				}

				doFileAccessedUpdate(cachedFile.getId());

				logger.info("Returning cached serviceFile " + uniqueKey);
				return returnFile;
			} catch (SQLException sqlException) {
				logger.error("There was an excuse attempting to get the cached serviceFile " + uniqueKey, sqlException);
				return null;
			}
		}, RepositoryAccessHelper.databaseExecutor);
	}

	public Promise<Boolean> containsKey(final String uniqueKey) throws IOException {
		return promiseCachedFile(uniqueKey).then(file -> file != null);
	}

	private void updateFilePath(final long cachedFileId, final String filePath) {
		try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
				logger.info("Updating serviceFile name of cached serviceFile with ID " + cachedFileId + " to " + filePath);

				repositoryAccessHelper
						.mapSql("UPDATE " + CachedFile.tableName + " SET " + CachedFile.FILE_NAME + " = @" + CachedFile.FILE_NAME + " WHERE id = @id")
						.addParameter(CachedFile.FILE_NAME, filePath)
						.addParameter("id", cachedFileId)
						.execute();

				closeableTransaction.setTransactionSuccessful();
			} catch (SQLException sqlException) {
				logger.error("There was an excuse trying to update the cached serviceFile with ID " + cachedFileId, sqlException);
				throw sqlException;
			}
		}
	}

	private void doFileAccessedUpdate(final long cachedFileId) {
		final long updateTime = System.currentTimeMillis();
		logger.info("Updating accessed time on cached serviceFile with ID " + cachedFileId + " to " + new Date(updateTime));

		try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
				repositoryAccessHelper
						.mapSql("UPDATE " + CachedFile.tableName + " SET " + CachedFile.LAST_ACCESSED_TIME + " = @" + CachedFile.LAST_ACCESSED_TIME + " WHERE id = @id")
						.addParameter(CachedFile.LAST_ACCESSED_TIME, updateTime)
						.addParameter("id", cachedFileId)
						.execute();

				closeableTransaction.setTransactionSuccessful();
			} catch (SQLException sqlException) {
				logger.error("There was an excuse trying to update the cached serviceFile with ID " + cachedFileId, sqlException);
				throw sqlException;
			}
		}
	}

	private CachedFile getCachedFile(final String uniqueKey) throws IOException {
		try (final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			try (final CloseableNonExclusiveTransaction closeableNonExclusiveTransaction = repositoryAccessHelper.beginNonExclusiveTransaction()) {
				final CachedFile cachedFile = repositoryAccessHelper
						.mapSql("SELECT * FROM " + CachedFile.tableName + cachedFileFilter)
						.addParameter(CachedFile.LIBRARY_ID, library.getId())
						.addParameter(CachedFile.CACHE_NAME, cacheName)
						.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
						.fetchFirst(CachedFile.class);

				closeableNonExclusiveTransaction.setTransactionSuccessful();
				return cachedFile;
			} catch (SQLException sqlException) {
				logger.error("There was an excuse getting the serviceFile with unique key " + uniqueKey, sqlException);
				return null;
			} catch (IOException e) {
				logger.error("There was an excuse opening the non exclusive transaction", e);
				throw e;
			}
		}
	}

	private long deleteCachedFile(final long cachedFileId) {
		try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
				logger.info("Deleting cached serviceFile with id " + cachedFileId);

				if (logger.isDebugEnabled())
					logger.debug("Cached serviceFile count: " + getTotalCachedFileCount(repositoryAccessHelper));

				final long executionResult =
						repositoryAccessHelper
								.mapSql("DELETE FROM " + CachedFile.tableName + " WHERE id = @id")
								.addParameter("id", cachedFileId)
								.execute();

				if (logger.isDebugEnabled())
					logger.debug("Cached serviceFile count: " + getTotalCachedFileCount(repositoryAccessHelper));

				closeableTransaction.setTransactionSuccessful();

				return executionResult;
			} catch (SQLException sqlException) {
				logger.warn("There was an excuse trying to delete the cached serviceFile with id " + cachedFileId, sqlException);
			}
		}

		return -1;
	}

	private static long getTotalCachedFileCount(RepositoryAccessHelper repositoryAccessHelper) {
		return repositoryAccessHelper.mapSql("SELECT COUNT(*) FROM " + CachedFile.tableName).execute();
	}

	// Creates a unique subdirectory of the designated app cache directory. Tries to use external
	// but if not mounted, falls back on internal storage.
	public static java.io.File getDiskCacheDir(final Context context, final String uniqueName) {
	    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
	    // otherwise use internal cache dir
		final java.io.File cacheDir =
	            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ?
            		context.getExternalCacheDir() :
                    context.getCacheDir();

	    return new java.io.File(cacheDir, uniqueName);
	}

	private static long getFreeDiskSpace(final Context context) {
		return getDiskCacheDir(context, null).getUsableSpace();
	}
}
