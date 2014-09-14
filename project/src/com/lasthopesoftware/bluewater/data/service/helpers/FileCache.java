package com.lasthopesoftware.bluewater.data.service.helpers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Environment;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.lasthopesoftware.bluewater.data.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.data.sqlite.objects.CachedFile;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;

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
	
	public void put(final String uniqueKey, final File file) {
		final SimpleTask<Void, Void, Void> putTask = new SimpleTask<Void, Void, Void>(new OnExecuteListener<Void, Void, Void>() {

			@Override
			public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
				final DatabaseHandler handler = new DatabaseHandler(mContext);
				try {
					CachedFile cachedFile = getCachedFile(handler, mLibrary.getId(), mCacheName, uniqueKey);
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
					
					cachedFile.setLastAccessedTime(new Date());
					
					try {
						handler.getAccessObject(CachedFile.class).createOrUpdate(cachedFile);
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
					final CachedFile cachedFile = getCachedFile(handler, mLibrary.getId(), mCacheName, uniqueKey);
					if (cachedFile != null) {
						cachedFile.setLastAccessedTime(new Date());
						try {
							handler.getAccessObject(CachedFile.class).update(cachedFile);
						} catch (SQLException e) {
							mLogger.error("Error updating cached file entity", e);
						}
					}
					return cachedFile != null ? new File(cachedFile.getFileName()) : null;
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
	
	private final static CachedFile getCachedFile(final DatabaseHandler handler, final int libraryId, final String cacheName, final String uniqueKey) {
		try {
			final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
			
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
			mLogger.error("SQLException", e);
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
