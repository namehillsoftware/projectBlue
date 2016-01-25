package com.lasthopesoftware.bluewater.servers.library.items.media.files.cached;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.objective.ObjectiveDroid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DiskFileCache {
	
	private final static long msInDay = 86400000L;
	
	private static final String cachedFileFilter =
			" WHERE " + CachedFile.LIBRARY_ID + " = @" + CachedFile.LIBRARY_ID +
			" AND " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME +
			" AND " + CachedFile.UNIQUE_KEY + " = @" + CachedFile.UNIQUE_KEY;

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

	public void put(final String uniqueKey, final File file, final byte[] fileData) {

		// Just execute this on the thread pool executor as it doesn't write to the database
		AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {

			@Override
			public final void run() {
				try {

					final FileOutputStream fos = new FileOutputStream(file);
					try {
						fos.write(fileData);
						fos.flush();
					} finally {
						fos.close();
					}

					put(uniqueKey, file);
				} catch (IOException e) {
					logger.error("Unable to write to file!", e);

					// Check if free space is too low and then attempt to free up enough space
					// to store image
					final long freeSpace = getFreeDiskSpace(context);
					if (freeSpace > maxSize) return;

					CacheFlusher.doFlushSynchronously(context, cacheName, maxSize - file.length());
					put(uniqueKey, file, fileData);
				}
			}
		});
	}

	private void put(final String uniqueKey, final File file) {
		RepositoryAccessHelper.databaseExecutor.execute(new Runnable() {

			@Override
			public void run() {
				RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {

					CachedFile cachedFile = getCachedFile(repositoryAccessHelper, library.getId(), cacheName, uniqueKey);
					if (cachedFile != null) {
						repositoryAccessHelper
								.mapSql("UPDATE " + CachedFile.tableName + " SET " + CachedFile.LAST_ACCESSED_TIME + " = @" + CachedFile.LAST_ACCESSED_TIME + " WHERE id = @id")
								.addParameter("id", cachedFile.getId())
								.addParameter(CachedFile.LAST_ACCESSED_TIME, System.currentTimeMillis())
								.execute();

						return;
					}

					final String cachedFileSqlInsert =
						InsertBuilder
							.fromTable(CachedFile.tableName)
							.addColumn(CachedFile.CACHE_NAME)
							.addColumn(CachedFile.FILE_NAME)
							.addColumn(CachedFile.FILE_SIZE)
							.addColumn(CachedFile.LIBRARY_ID)
							.addColumn(CachedFile.UNIQUE_KEY)
							.addColumn(CachedFile.CREATED_TIME)
							.addColumn(CachedFile.LAST_ACCESSED_TIME)
							.build();

					final ObjectiveDroid sqlInsertMapper = repositoryAccessHelper.mapSql(cachedFileSqlInsert);

					try {
						sqlInsertMapper.addParameter(CachedFile.FILE_NAME, file.getCanonicalPath());
					} catch (IOException e) {
						logger.error("There was an error getting the canonical path for " + file, e);
						return;
					}

					sqlInsertMapper
						.addParameter(CachedFile.CACHE_NAME, cacheName)
						.addParameter(CachedFile.FILE_SIZE, file.length())
						.addParameter(CachedFile.LIBRARY_ID, library.getId())
						.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
						.addParameter(CachedFile.CREATED_TIME, System.currentTimeMillis())
						.addParameter(CachedFile.LAST_ACCESSED_TIME, System.currentTimeMillis())
						.execute();

				} finally {
					repositoryAccessHelper.close();
					CacheFlusher.doFlush(context, cacheName, maxSize);
				}
			}
		});
	}

	public File get(final String uniqueKey) {
		final FluentTask<Void, Void, File> getTask = new FluentTask<Void, Void, File>() {

			@Override
			protected File executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final CachedFile cachedFile = getCachedFile(repositoryAccessHelper, library.getId(), cacheName, uniqueKey);

					if (cachedFile == null) return null;

					final File returnFile = new File(cachedFile.getFileName());
					if (!returnFile.exists()) {
						deleteCachedFile(repositoryAccessHelper, cachedFile.getId());
						return null;
					}

					// Remove the file and return null if it's past its expired time
					if (cachedFile.getCreatedTime() < System.currentTimeMillis() - expirationTime) {
						if (returnFile.delete())
							deleteCachedFile(repositoryAccessHelper, cachedFile.getId());

						return null;
					}

					doFileAccessedUpdate(uniqueKey);

					return returnFile;
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};

		try {
			return getTask.get(RepositoryAccessHelper.databaseExecutor);
		} catch (Exception e) {
			logger.error("There was an error running the database task.", e);
			return null;
		}
	}

	public boolean containsKey(final String uniqueKey) {
		return get(uniqueKey) != null;
	}

	private void doFileAccessedUpdate(final String uniqueKey) {
		final long updateTime = System.currentTimeMillis();
		RepositoryAccessHelper.databaseExecutor.execute(new Runnable() {

			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					repositoryAccessHelper
							.mapSql("UPDATE " + CachedFile.tableName + " SET " + CachedFile.LAST_ACCESSED_TIME + " = @" + CachedFile.LAST_ACCESSED_TIME + cachedFileFilter)
							.addParameter(CachedFile.LAST_ACCESSED_TIME, updateTime)
							.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
							.addParameter(CachedFile.CACHE_NAME, cacheName)
							.addParameter(CachedFile.LIBRARY_ID, library.getId())
							.execute();

				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	private static CachedFile getCachedFile(final RepositoryAccessHelper repositoryAccessHelper, final int libraryId, final String cacheName, final String uniqueKey) {
		return repositoryAccessHelper
				.mapSql(
						"SELECT * FROM " + CachedFile.tableName +
								" WHERE " + CachedFile.LIBRARY_ID + " = @" + CachedFile.LIBRARY_ID +
								" AND " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME +
								" AND " + CachedFile.UNIQUE_KEY + " = @" + CachedFile.UNIQUE_KEY)
				.addParameter(CachedFile.LIBRARY_ID, libraryId)
				.addParameter(CachedFile.CACHE_NAME, cacheName)
				.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
				.fetchFirst(CachedFile.class);
	}

	private static long deleteCachedFile(final RepositoryAccessHelper repositoryAccessHelper, final int cachedFileId) {
		return repositoryAccessHelper
				.mapSql("DELETE FROM " + CachedFile.tableName + " WHERE id = @id")
				.addParameter("id", cachedFileId)
				.execute();
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
