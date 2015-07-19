package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.store.CachedFile;
import com.lasthopesoftware.bluewater.servers.store.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class DiskFileCache {
	
	private final static long MS_IN_DAY = 86400000L;
	
	private final static Logger mLogger = LoggerFactory.getLogger(DiskFileCache.class); 
	
	private final Context mContext;
	private final Library mLibrary;
	private final String mCacheName;
	private final long mMaxSize;
	private final long mExpirationTime;
	
	public DiskFileCache(final Context context, final Library library, final String cacheName, final int expirationDays, final long maxSize) {
		mContext = context;
		mCacheName = cacheName;
		mMaxSize = maxSize;
		mLibrary = library;
		mExpirationTime = expirationDays * MS_IN_DAY;
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
					mLogger.error("Unable to write to file!", e);

					// Check if free space is too low and then attempt to free up enough space
					// to store image
					final long freeSpace = getFreeDiskSpace(mContext);
					if (freeSpace > mMaxSize) return;

					CacheFlusher.doFlushSynchronously(mContext, mCacheName, mMaxSize - file.length());
					put(uniqueKey, file, fileData);
				}
			}
		});
	}
	
	public void put(final String uniqueKey, final File file) {
		DatabaseHandler.databaseExecutor.execute(new Runnable() {

			@Override
			public void run() {
				final DatabaseHandler handler = new DatabaseHandler(mContext);
				try {
					final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
										
					CachedFile cachedFile = getCachedFile(cachedFileAccess, mLibrary.getId(), mCacheName, uniqueKey);
					if (cachedFile == null) {
						cachedFile = new CachedFile();
						cachedFile.setCacheName(mCacheName);
						try {
							cachedFile.setFileName(file.getCanonicalPath());
						} catch (IOException e) {
							mLogger.error("There was an error reading the canonical path", e);
							return;
						}
						cachedFile.setFileSize(file.length());
						cachedFile.setLibrary(mLibrary);
						cachedFile.setUniqueKey(uniqueKey);
						cachedFile.setCreatedTime(System.currentTimeMillis());
					}
					
					cachedFile.setLastAccessedTime(System.currentTimeMillis());
					
					try {
						cachedFileAccess.createOrUpdate(cachedFile);
					} catch (SQLException e) {
						mLogger.error("Error updating cached file", e);
					}
				} catch (SQLException se) {
					mLogger.warn("Couldn't get database access object.");
				} finally {
					handler.close();
					CacheFlusher.doFlush(mContext, mCacheName, mMaxSize);
				}
			}
		});
	}
	
	public File get(final String uniqueKey) {
		final SimpleTask<Void, Void, File> getTask = new SimpleTask<>(new OnExecuteListener<Void, Void, File>() {

			@Override
			public File onExecute(ISimpleTask<Void, Void, File> owner, Void... params) throws Exception {
				final DatabaseHandler handler = new DatabaseHandler(mContext);
				try {
					final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
					
					final CachedFile cachedFile = getCachedFile(cachedFileAccess, mLibrary.getId(), mCacheName, uniqueKey);
					
					if (cachedFile == null) return null;
										
					final File returnFile = new File(cachedFile.getFileName());
					if (returnFile == null || !returnFile.exists()) {					
						cachedFileAccess.delete(cachedFile);
						return null;
					}
					
					// Remove the file and return null if it's past its expired time
					if (cachedFile.getCreatedTime() < System.currentTimeMillis() - mExpirationTime) {
						cachedFileAccess.delete(cachedFile);
						returnFile.delete();
						return null;
					}
										
					doFileAccessedUpdate(uniqueKey);
					
					return returnFile;
				} finally {
					handler.close();
				}
			}
			
		});
		
		try {
			return getTask.execute(DatabaseHandler.databaseExecutor).get();
		} catch (Exception e) {
			mLogger.error("There was an error running the database task.", e);
			return null;
		}
	}
	
	public boolean containsKey(final String uniqueKey) {
		return get(uniqueKey) != null;
	}
	
	private final void doFileAccessedUpdate(final String uniqueKey) {
		final long updateTime = System.currentTimeMillis();
		DatabaseHandler.databaseExecutor.execute(new Runnable() {

			@Override
			public void run() {
				final DatabaseHandler handler = new DatabaseHandler(mContext);
				try {
					final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
					final CachedFile cachedFile = getCachedFile(cachedFileAccess, mLibrary.getId(), mCacheName, uniqueKey);
					if (cachedFile == null) return;
					cachedFile.setLastAccessedTime(updateTime);
					try {
						cachedFileAccess.update(cachedFile);
					} catch (SQLException e) {
						mLogger.error("Error updating file accessed time.", e);
					}
				} catch (SQLException e) {
					mLogger.error("Error getting database access object.", e);
				} finally {
					handler.close();
				}
			}
		});
	}
	
	private final static CachedFile getCachedFile(final Dao<CachedFile, Integer> cachedFileAccess, final int libraryId, final String cacheName, final String uniqueKey) {
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
			mLogger.error("Error retrieving file", e);
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

	public static long getFreeDiskSpace(final Context context) {
		return getDiskCacheDir(context, null).getUsableSpace();
	}
}
