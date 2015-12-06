package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.repository.CachedFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.OnExecuteListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class DiskFileCache {
	
	private final static long msInDay = 86400000L;
	
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
					final Dao<CachedFile, Integer> cachedFileAccess = repositoryAccessHelper.getDataAccess(CachedFile.class);
										
					CachedFile cachedFile = getCachedFile(cachedFileAccess, library.getId(), cacheName, uniqueKey);
					if (cachedFile == null) {
						cachedFile = new CachedFile();
						cachedFile.setCacheName(cacheName);
						try {
							cachedFile.setFileName(file.getCanonicalPath());
						} catch (IOException e) {
							logger.error("There was an error reading the canonical path", e);
							return;
						}
						cachedFile.setFileSize(file.length());
						cachedFile.setLibraryId(library.getId());
						cachedFile.setUniqueKey(uniqueKey);
						cachedFile.setCreatedTime(System.currentTimeMillis());
					}
					
					cachedFile.setLastAccessedTime(System.currentTimeMillis());
					
					try {
						cachedFileAccess.createOrUpdate(cachedFile);
					} catch (SQLException e) {
						logger.error("Error updating cached file", e);
					}
				} catch (SQLException se) {
					logger.warn("Couldn't get database access object.");
				} finally {
					repositoryAccessHelper.close();
					CacheFlusher.doFlush(context, cacheName, maxSize);
				}
			}
		});
	}
	
	public File get(final String uniqueKey) {
		final FluentTask<Void, Void, File> getTask = new FluentTask<>(new OnExecuteListener<Void, Void, File>() {

			@Override
			public File onExecute(FluentTask<Void, Void, File> owner, Void... params) throws Exception {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<CachedFile, Integer> cachedFileAccess = repositoryAccessHelper.getDataAccess(CachedFile.class);

					final CachedFile cachedFile = getCachedFile(cachedFileAccess, library.getId(), cacheName, uniqueKey);

					if (cachedFile == null) return null;

					final File returnFile = new File(cachedFile.getFileName());
					if (!returnFile.exists()) {
						cachedFileAccess.delete(cachedFile);
						return null;
					}

					// Remove the file and return null if it's past its expired time
					if (cachedFile.getCreatedTime() < System.currentTimeMillis() - expirationTime) {
						cachedFileAccess.delete(cachedFile);
						returnFile.delete();
						return null;
					}

					doFileAccessedUpdate(uniqueKey);

					return returnFile;
				} finally {
					repositoryAccessHelper.close();
				}
			}
			
		});
		
		try {
			return getTask.execute(RepositoryAccessHelper.databaseExecutor).get();
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
					final Dao<CachedFile, Integer> cachedFileAccess = repositoryAccessHelper.getDataAccess(CachedFile.class);
					final CachedFile cachedFile = getCachedFile(cachedFileAccess, library.getId(), cacheName, uniqueKey);
					if (cachedFile == null) return;
					cachedFile.setLastAccessedTime(updateTime);
					try {
						cachedFileAccess.update(cachedFile);
					} catch (SQLException e) {
						logger.error("Error updating file accessed time.", e);
					}
				} catch (SQLException e) {
					logger.error("Error getting database access object.", e);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}
	
	private static CachedFile getCachedFile(final Dao<CachedFile, Integer> cachedFileAccess, final int libraryId, final String cacheName, final String uniqueKey) {
		try {
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.where()
						.eq(CachedFile.LIBRARY_ID, new SelectArg(libraryId))
						.and()
						.eq(CachedFile.CACHE_NAME, new SelectArg(cacheName))
						.and()
						.eq(CachedFile.UNIQUE_KEY, new SelectArg(uniqueKey)).prepare();
			
			return cachedFileAccess.queryForFirst(preparedQuery);			
		} catch (SQLException e) {
			logger.error("Error retrieving file", e);
			return null;
		}
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
