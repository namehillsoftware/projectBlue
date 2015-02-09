package com.lasthopesoftware.bluewater.disk.cache;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.CachedFile;

/**
 * Flush a given cache until it reaches the given target size
 * @author david
 *
 */
public class CacheFlusher implements Runnable {

	private final static Logger mLogger = LoggerFactory.getLogger(CacheFlusher.class);
	
	private final Context mContext;
	private final String mCacheName;
	private final long mTargetSize;
	
	/*
	 * Flush a given cache until it reaches the given target size
	 */
	public static void doFlush(final Context context, final String cacheName, final long targetSize) {
		DatabaseHandler.databaseExecutor.execute(new CacheFlusher(context, cacheName, targetSize));
	}
	
	private CacheFlusher(final Context context, final String cacheName, final long targetSize) {
		mContext = context;
		mCacheName = cacheName;
		mTargetSize = targetSize;
	}
	
	@Override
	public final void run() {
		final DatabaseHandler handler = new DatabaseHandler(mContext);
		try {
			final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
			
			if (getCachedFileSizeFromDatabase(cachedFileAccess) <= mTargetSize) return;
			
			while (getCachedFileSizeFromDatabase(cachedFileAccess) > mTargetSize) {
				final CachedFile cachedFile = getOldestCachedFile(cachedFileAccess);
				if (cachedFile != null)
					deleteCachedFile(cachedFileAccess, cachedFile);
			}
			
			// Remove any files in the cache dir but not in the database
			final File cacheDir = DiskFileCache.getDiskCacheDir(mContext, mCacheName);
			
			if (cacheDir == null || !cacheDir.exists()) return;
			
			final File[] filesInCacheDir = cacheDir.listFiles();
			
			// If the # of files in the cache dir is equal to the database size, then
			// hypothetically (and good enough for our purposes), they are in sync and we don't need
			// to do additional processing
			if (filesInCacheDir == null || filesInCacheDir.length == getCachedFileCount(cachedFileAccess))
				return;
			
			for (File fileInCacheDir : filesInCacheDir) {
				try {
					if (getCachedFileByFilename(cachedFileAccess, fileInCacheDir.getCanonicalPath()) != null) continue;
				} catch (IOException e) {
					mLogger.warn("Issue getting canonical file path.");
				}
				fileInCacheDir.delete();
			}
		} catch (SQLException accessException) {
			mLogger.error("Error accessing cache", accessException);
		} finally {
			handler.close();
		}
	}

	private final long getCachedFileSizeFromDatabase(final Dao<CachedFile, Integer> cachedFileAccess) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.selectRaw("SUM(" + CachedFile.FILE_SIZE + ")")
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg())
						.prepare();
			
			return cachedFileAccess.queryRawValue(preparedQuery.getStatement(), mCacheName);
		} catch (SQLException e) {
			mLogger.error("Error getting file size", e);
			return -1;
		}
	}
	
	private final long getCacheSizeBetweenTimes(final Dao<CachedFile, Integer> cachedFileAccess, final long startTime, final long endTime) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.selectRaw("SUM(" + CachedFile.FILE_SIZE + ")")
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg())
						.and()
						.between(CachedFile.CREATED_TIME, new SelectArg(), new SelectArg())
						.prepare();
			
			return cachedFileAccess.queryRawValue(preparedQuery.getStatement(), mCacheName, String.valueOf(startTime), String.valueOf(endTime));
		} catch (SQLException e) {
			mLogger.error("Error getting file size", e);
			return -1;
		}
	}
	
	private final CachedFile getOldestCachedFile(final Dao<CachedFile, Integer> cachedFileAccess) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.orderBy(CachedFile.LAST_ACCESSED_TIME, true)
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg(mCacheName))
						.prepare();
			
			return cachedFileAccess.queryForFirst(preparedQuery);			
		} catch (SQLException e) {
			mLogger.error("Error getting oldest cached file", e);
			return null;
		}
	}
	
	private final long getCachedFileCount(final Dao<CachedFile, Integer> cachedFileAccess) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.setCountOf(true)
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg(mCacheName))
						.prepare();
			
			return cachedFileAccess.countOf(preparedQuery);
		} catch (SQLException e) {
			mLogger.error("Error getting file count", e);
			return -1;
		}
	}
	
	private final static CachedFile getCachedFileByFilename(final Dao<CachedFile, Integer> cachedFileAccess, final String fileName) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.where()
						.eq(CachedFile.FILE_NAME, new SelectArg(fileName))
						.prepare();
			
			return cachedFileAccess.queryForFirst(preparedQuery);	
		} catch (SQLException e) {
			mLogger.error("Error getting cached file by filename", e);
			return null;
		}
	}
	
	private final boolean deleteCachedFile(final Dao<CachedFile, Integer> cachedFileAccess, final CachedFile cachedFile) {
		final File fileToDelete = new File(cachedFile.getFileName());
		if (fileToDelete.exists()) 
			fileToDelete.delete();
		
		try {
			cachedFileAccess.delete(cachedFile);
			return true;
		} catch (SQLException deleteException) {
			mLogger.error("Error deleting file pointer from database", deleteException);
		}
		
		return false;
	}
}
