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
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.lazyj.Lazy;
import com.vedsoft.objective.droid.ObjectiveDroid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

	public DiskFileCache(final Context context, final Library library, final String cacheName, final int expirationDays, final long maxSize) {
		this.context = context;
		this.cacheName = cacheName;
		this.maxSize = maxSize;
		this.library = library;
		expirationTime = expirationDays * msInDay;
	}

	public void put(final String uniqueKey, final byte[] fileData) throws IOException {

		// Just execute this on the thread pool executor as it doesn't write to the database
		final FluentTask<Void, Void, Void> putTask = new FluentTask<Void, Void, Void>() {

			@Override
			protected Void executeInBackground(Void[] params) {
				if (isCancelled()) return null;

				final java.io.File cacheDir = DiskFileCache.getDiskCacheDir(context, cacheName);
				if (!cacheDir.exists() && !cacheDir.mkdirs() || isCancelled()) return null;

				final File file = new File(cacheDir, (String.valueOf(library.getId()) + "-" + cacheName + "-" + uniqueKey).hashCode() + ".cache");

				do {
					if (isCancelled()) return null;

					try {

						try (FileOutputStream fos = new FileOutputStream(file)) {
							fos.write(fileData);
							fos.flush();
						}

						putIntoDatabase(uniqueKey, file);
						return null;
					} catch (IOException e) {
						logger.error("Unable to write to file!", e);

						// Check if free space is too low and then attempt to free up enough space
						// to store image
						if (getFreeDiskSpace(context) > maxSize) {
							setException(e);
							return null;
						}

						try {
							new CacheFlusherTask(context, cacheName, maxSize - file.length()).get();
						} catch (ExecutionException | InterruptedException ignored) {
							setException(e);
							return null;
						}
					}

					if (isCancelled()) return null;
				} while (getFreeDiskSpace(context) >= file.length());

				return null;
			}
		};

		try {
			putTask.get(AsyncTask.THREAD_POOL_EXECUTOR);
		} catch (ExecutionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof IOException)
				throw (IOException)cause;

			logger.error("There was an error putting the file with the unique key " + uniqueKey + " into the cache.", e);
		} catch (InterruptedException e) {
			logger.warn("Putting the file with the unique key " + uniqueKey + " into the cache was interrupted.", e);
		}
	}

	private void putIntoDatabase(final String uniqueKey, final File file) {
		RepositoryAccessHelper.databaseExecutor.execute(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {

				final CachedFile cachedFile;
				try {
					cachedFile = getCachedFile(repositoryAccessHelper, library.getId(), cacheName, uniqueKey);
				} catch (IOException e) {
					return;
				}

				if (cachedFile != null) {
					doFileAccessedUpdate(repositoryAccessHelper, cachedFile.getId());
					return;
				}

				final ObjectiveDroid sqlInsertMapper = repositoryAccessHelper.mapSql(cachedFileSqlInsert.getObject());

				try {
					sqlInsertMapper.addParameter(CachedFile.FILE_NAME, file.getCanonicalPath());
				} catch (IOException e) {
					logger.error("There was an error getting the canonical path for " + file, e);
					return;
				}

				try {
					sqlInsertMapper
							.addParameter(CachedFile.CACHE_NAME, cacheName)
							.addParameter(CachedFile.FILE_SIZE, file.length())
							.addParameter(CachedFile.LIBRARY_ID, library.getId())
							.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
							.addParameter(CachedFile.CREATED_TIME, System.currentTimeMillis())
							.addParameter(CachedFile.LAST_ACCESSED_TIME, System.currentTimeMillis())
							.execute();
				} catch (SQLException sqlException) {
					logger.warn("There was an error inserting the cached file with the unique key " + uniqueKey, sqlException);
				}
			} finally {
				new CacheFlusherTask(context, cacheName, maxSize).execute();
			}
		});
	}

	public File get(final String uniqueKey) throws IOException {
		final FluentTask<Void, Void, File> getFileTask = new FluentTask<Void, Void, File>() {

			@Override
			protected File executeInBackground(Void... params) {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					logger.info("Getting cached file " + uniqueKey);
					try {
						final CachedFile cachedFile;
						try {
							cachedFile = getCachedFile(repositoryAccessHelper, library.getId(), cacheName, uniqueKey);
						} catch (IOException e) {
							setException(e);
							return null;
						}

						if (cachedFile == null) return null;

						final File returnFile = new File(cachedFile.getFileName());
						logger.info("Checking if " + cachedFile.getFileName() + " exists.");
						if (!returnFile.exists()) {
							logger.warn("Cached file `" + cachedFile.getFileName() + "` doesn't exist! Removing from database.");
							deleteCachedFile(repositoryAccessHelper, cachedFile.getId());

							return null;
						}

						// Remove the file and return null if it's past its expired time
						if (cachedFile.getCreatedTime() < System.currentTimeMillis() - expirationTime) {
							logger.info("Cached file " + uniqueKey + " expired. Deleting.");
							if (returnFile.delete()) {
								deleteCachedFile(repositoryAccessHelper, cachedFile.getId());
								return null;
							}

							setException(new IOException("Unable to delete file " + returnFile.getAbsolutePath()));
						}

						doFileAccessedUpdate(repositoryAccessHelper, cachedFile.getId());

						logger.info("Returning cached file " + uniqueKey);
						return returnFile;
					} catch (SQLException sqlException) {
						logger.error("There was an error attempting to get the cached file " + uniqueKey, sqlException);
						return null;
					}
				}
			}
		};

		try {
			return getFileTask.get(RepositoryAccessHelper.databaseExecutor, 60, TimeUnit.SECONDS);
		} catch (TimeoutException te) {
			logger.warn("Getting the cached file '" + uniqueKey + "'  timed out.", te);

			if (!getFileTask.isCancelled())
				getFileTask.cancel(true);
		} catch (Exception e) {
			logger.error("There was an error running the database task.", e);

			if (!getFileTask.isCancelled())
				getFileTask.cancel(true);

			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}

		return null;
	}

	public boolean containsKey(final String uniqueKey) throws IOException {
		return get(uniqueKey) != null;
	}

	private static void doFileAccessedUpdate(final RepositoryAccessHelper repositoryAccessHelper, final long cachedFileId) {
		final long updateTime = System.currentTimeMillis();
		logger.info("Updating accessed time on cached file with ID " + cachedFileId + " to " + new Date(updateTime));

		try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			repositoryAccessHelper
					.mapSql("UPDATE " + CachedFile.tableName + " SET " + CachedFile.LAST_ACCESSED_TIME + " = @" + CachedFile.LAST_ACCESSED_TIME + " WHERE id = @id")
					.addParameter(CachedFile.LAST_ACCESSED_TIME, updateTime)
					.addParameter("id", cachedFileId)
					.execute();

			closeableTransaction.setTransactionSuccessful();
		} catch (SQLException sqlException) {
			logger.error("There was an error trying to update the cached file with ID " + cachedFileId, sqlException);
		}
	}

	private static CachedFile getCachedFile(final RepositoryAccessHelper repositoryAccessHelper, final int libraryId, final String cacheName, final String uniqueKey) throws IOException {
		try (final CloseableNonExclusiveTransaction closeableNonExclusiveTransaction = repositoryAccessHelper.beginNonExclusiveTransaction()) {
			final CachedFile cachedFile = repositoryAccessHelper
					.mapSql("SELECT * FROM " + CachedFile.tableName + cachedFileFilter)
					.addParameter(CachedFile.LIBRARY_ID, libraryId)
					.addParameter(CachedFile.CACHE_NAME, cacheName)
					.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
					.fetchFirst(CachedFile.class);

			closeableNonExclusiveTransaction.setTransactionSuccessful();
			return cachedFile;
		} catch (IOException e) {
			logger.error("There was an error opening the non exclusive transaction", e);
			throw e;
		}
	}

	private static long deleteCachedFile(final RepositoryAccessHelper repositoryAccessHelper, final long cachedFileId) {
		try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			logger.info("Deleting cached file with id " + cachedFileId);

			if (logger.isDebugEnabled()) {
				final long cachedFileCount = repositoryAccessHelper.mapSql("SELECT COUNT(*) FROM " + CachedFile.tableName).execute();

				logger.debug("Cached file count: " + cachedFileCount);
			}

			final long executionResult =
					repositoryAccessHelper
							.mapSql("DELETE FROM " + CachedFile.tableName + " WHERE id = @id")
							.addParameter("id", cachedFileId)
							.execute();
			closeableTransaction.setTransactionSuccessful();

			if (logger.isDebugEnabled()) {
				final long cachedFileCount = repositoryAccessHelper.mapSql("SELECT COUNT(*) FROM " + CachedFile.tableName).execute();

				logger.debug("Cached file count: " + cachedFileCount);
			}

			return executionResult;
		} catch (SQLException sqlException) {
			logger.warn("There was an error trying to delete the cached file with id " + cachedFileId);
		}

		return -1;
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
