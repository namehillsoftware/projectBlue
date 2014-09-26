package com.lasthopesoftware.bluewater.data.service.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.lasthopesoftware.bluewater.data.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.data.sqlite.objects.CachedFile;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class FileCache {
	
	private final static Logger mLogger = LoggerFactory.getLogger(FileCache.class); 
	
	private final Context mContext;
	private final Library mLibrary;
	private final String mCacheName;
	private final int mMaxSize;
	
	public FileCache(Context context, Library library, String cacheName, int maxSize) {
		mContext = context;
		mCacheName = cacheName;
		mMaxSize = maxSize;
		mLibrary = library;
	}
	
	public void put(final String uniqueKey, final File file, final byte[] fileData) {
		final SimpleTask<Void, Void, Void> writeFileTask = new SimpleTask<Void, Void, Void>(new OnExecuteListener<Void, Void, Void>() {

			@Override
			public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
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
				}
				
				return null;
			}
			
		});
		
		// Just execute this on the thread pool executor as it doesn't write to the database
		writeFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	public void put(final String uniqueKey, final File file) {
		final SimpleTask<Void, Void, Void> putTask = new SimpleTask<Void, Void, Void>(new OnExecuteListener<Void, Void, Void>() {

			@Override
			public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
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
							return null;
						}
						cachedFile.setFileSize(file.length());
						cachedFile.setLibrary(mLibrary);
						cachedFile.setUniqueKey(uniqueKey);
					}
					
					cachedFile.setLastAccessedTime(System.currentTimeMillis());
					
					try {
						cachedFileAccess.createOrUpdate(cachedFile);
					} catch (SQLException e) {
						mLogger.error("Error updating cached file", e);
					}
				} finally {
					handler.close();
					FlushCacheTask.doFlush(mContext, mCacheName, mMaxSize);
				}
				
				return null;
			}
		});
		
		putTask.executeOnExecutor(DatabaseHandler.databaseExecutor);
	}
	
	public File get(final String uniqueKey) {
		final SimpleTask<Void, Void, File> getTask = new SimpleTask<Void, Void, File>(new OnExecuteListener<Void, Void, File>() {

			@Override
			public File onExecute(ISimpleTask<Void, Void, File> owner, Void... params) throws Exception {
				final DatabaseHandler handler = new DatabaseHandler(mContext);
				try {
					final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
					
					final CachedFile cachedFile = getCachedFile(cachedFileAccess, mLibrary.getId(), mCacheName, uniqueKey);
					
					if (cachedFile == null) return null;
					
					doFileAccessedUpdate(uniqueKey);
					
					final File returnFile = new File(cachedFile.getFileName());
					if (returnFile == null || !returnFile.exists()) {					
						cachedFileAccess.delete(cachedFile);
						return null;
					}
					
					return returnFile;
				} finally {
					handler.close();
				}
			}
			
		});
		
		try {
			return getTask.executeOnExecutor(DatabaseHandler.databaseExecutor).get();
		} catch (InterruptedException | ExecutionException e) {
			mLogger.error("There was an error running the database task.", e);
			return null;
		}
	}
	
	public boolean containsKey(final String uniqueKey) {
		return get(uniqueKey) != null;
	}
	
	private final void doFileAccessedUpdate(final String uniqueKey) {
		final long updateTime = System.currentTimeMillis();
		final SimpleTask<Void, Void, Void> fileAccessUpdateTask = new SimpleTask<Void, Void, Void>(new OnExecuteListener<Void, Void, Void>() {

			@Override
			public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
				final DatabaseHandler handler = new DatabaseHandler(mContext);
				try {
					final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
					final CachedFile cachedFile = getCachedFile(cachedFileAccess, mLibrary.getId(), mCacheName, uniqueKey);
					if (cachedFile == null) return null;
					cachedFile.setLastAccessedTime(updateTime);
					try {
						cachedFileAccess.update(cachedFile);
					} catch (SQLException e) {
						mLogger.error("Error updating file accessed time", e);
					}
				} finally {
					handler.close();
				}
				return null;
			}
		});
		
		fileAccessUpdateTask.executeOnExecutor(DatabaseHandler.databaseExecutor);
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
	public final static java.io.File getDiskCacheDir(final Context context, final String uniqueName) {
	    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
	    // otherwise use internal cache dir
	    final String cachePath =
	            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ?
            		context.getExternalCacheDir().getPath() :
                    context.getCacheDir().getPath();

	    return new java.io.File(cachePath + java.io.File.separator + uniqueName);
	}
}
