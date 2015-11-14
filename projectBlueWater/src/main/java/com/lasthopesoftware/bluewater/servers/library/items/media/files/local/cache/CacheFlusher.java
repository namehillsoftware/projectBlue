package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.repository.CachedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Flush a given cache until it reaches the given target size
 * @author david
 *
 */
public class CacheFlusher implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(CacheFlusher.class);
	
	private final Context context;
	private final String cacheName;
	private final long targetSize;
	
	/*
	 * Flush a given cache until it reaches the given target size
	 */
	public static void doFlush(final Context context, final String cacheName, final long targetSize) {
		RepositoryAccessHelper.databaseExecutor.execute(new CacheFlusher(context, cacheName, targetSize));
	}

	public static void doFlushSynchronously(final Context context, final String cacheName, final long targetSize) {
		(new CacheFlusher(context, cacheName, targetSize)).run();
	}
	
	private CacheFlusher(final Context context, final String cacheName, final long targetSize) {
		this.context = context;
		this.cacheName = cacheName;
		this.targetSize = targetSize;
	}
	
	@Override
	public final void run() {
		final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
		try {
			final Dao<CachedFile, Integer> cachedFileAccess = repositoryAccessHelper.getDataAccess(CachedFile.class);
			
			if (getCachedFileSizeFromDatabase(cachedFileAccess) <= targetSize) return;
			
			while (getCachedFileSizeFromDatabase(cachedFileAccess) > targetSize) {
				final CachedFile cachedFile = getOldestCachedFile(cachedFileAccess);
				if (cachedFile != null)
					deleteCachedFile(cachedFileAccess, cachedFile);
			}
			
			// Remove any files in the cache dir but not in the database
			final File cacheDir = DiskFileCache.getDiskCacheDir(context, cacheName);
			
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
					logger.warn("Issue getting canonical file path.");
				}
				fileInCacheDir.delete();
			}
		} catch (SQLException accessException) {
			logger.error("Error accessing cache", accessException);
		} finally {
			repositoryAccessHelper.close();
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
			
			return cachedFileAccess.queryRawValue(preparedQuery.getStatement(), cacheName);
		} catch (SQLException e) {
			logger.error("Error getting file size", e);
			return -1;
		}
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
//			logger.error("Error getting file size", e);
//			return -1;
//		}
//	}
	
	private final CachedFile getOldestCachedFile(final Dao<CachedFile, Integer> cachedFileAccess) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.orderBy(CachedFile.LAST_ACCESSED_TIME, true)
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg(cacheName))
						.prepare();
			
			return cachedFileAccess.queryForFirst(preparedQuery);			
		} catch (SQLException e) {
			logger.error("Error getting oldest cached file", e);
			return null;
		}
	}
	
	private final long getCachedFileCount(final Dao<CachedFile, Integer> cachedFileAccess) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.setCountOf(true)
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg(cacheName))
						.prepare();
			
			return cachedFileAccess.countOf(preparedQuery);
		} catch (SQLException e) {
			logger.error("Error getting file count", e);
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
			logger.error("Error getting cached file by filename", e);
			return null;
		}
	}
	
	private final static boolean deleteCachedFile(final Dao<CachedFile, Integer> cachedFileAccess, final CachedFile cachedFile) {
		final File fileToDelete = new File(cachedFile.getFileName());
		if (fileToDelete.exists()) 
			fileToDelete.delete();
		
		try {
			cachedFileAccess.delete(cachedFile);
			return true;
		} catch (SQLException deleteException) {
			logger.error("Error deleting file pointer from database", deleteException);
		}
		
		return false;
	}
}
