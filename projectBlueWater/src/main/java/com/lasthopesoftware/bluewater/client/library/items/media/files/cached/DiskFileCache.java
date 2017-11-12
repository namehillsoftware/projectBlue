package com.lasthopesoftware.bluewater.client.library.items.media.files.cached;

import android.content.Context;
import android.database.SQLException;
import android.os.Environment;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access.ICachedFilesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.IDiskFileAccessTimeUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.IDiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.CachedFileOutputStream;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLDataException;

public class DiskFileCache {

	private final static Logger logger = LoggerFactory.getLogger(DiskFileCache.class);

	private final Context context;
	private final IDiskFileCacheConfiguration diskFileCacheConfiguration;
	private final IDiskFileCachePersistence diskFileCachePersistence;
	private final ICachedFilesProvider cachedFilesProvider;
	private final IDiskFileAccessTimeUpdater diskFileAccessTimeUpdater;

	private final long expirationTime;

	private final CreateAndHold<File> lazyDiskCacheDir = new AbstractSynchronousLazy<File>() {
		@Override
		protected File create() throws Exception {
			final java.io.File cacheDir = new File(DiskFileCache.getDiskCacheDir(context, diskFileCacheConfiguration.getCacheName()), String.valueOf(diskFileCacheConfiguration.getLibrary().getId()));
			if (!cacheDir.exists() && !cacheDir.mkdirs()) return null;

			return cacheDir;
		}
	};

	public DiskFileCache(final Context context, IDiskFileCacheConfiguration diskFileCacheConfiguration, IDiskFileCachePersistence diskFileCachePersistence, ICachedFilesProvider cachedFilesProvider, IDiskFileAccessTimeUpdater diskFileAccessTimeUpdater) {
		this.context = context;
		this.diskFileCacheConfiguration = diskFileCacheConfiguration;

		expirationTime = diskFileCacheConfiguration.getCacheExpirationDays() != null
			? diskFileCacheConfiguration.getCacheExpirationDays().toStandardDuration().getMillis()
			: -1;

		this.diskFileCachePersistence = diskFileCachePersistence;
		this.cachedFilesProvider = cachedFilesProvider;
		this.diskFileAccessTimeUpdater = diskFileAccessTimeUpdater;
	}

	public Promise<CachedFile> put(final String uniqueKey, final byte[] fileData) {
		final Promise<CachedFile> putPromise = promiseCachedFileOutputStream(uniqueKey)
			.eventually(cachedFileOutputStream -> writeCachedFileWithRetries(cachedFileOutputStream, fileData));

		putPromise.excuse(e -> {
			logger.error("There was an error putting the serviceFile with the unique key " + uniqueKey + " into the cache.", e);

			return null;
		});

		return putPromise;
	}

	private Promise<CachedFile> writeCachedFileWithRetries(CachedFileOutputStream cachedFileOutputStream, byte[] fileData) {
		final Promise<CachedFile> cachedFileWritePromise = cachedFileOutputStream
			.promiseWrite(fileData, 0, fileData.length)
			.eventually(CachedFileOutputStream::flush)
			.eventually(fos -> {
				fos.close();
				return fos.commitToCache();
			});

		final Promise<CachedFile> retryWritePromise = cachedFileWritePromise
			.excuse(e -> {
				logger.error("Unable to promiseWrite to serviceFile!", e);

				cachedFileOutputStream.close();
				return e;
			})
			.eventually(e -> {
				if (!(e instanceof IOException)) return Promise.empty();

				// Check if free space is too low and then attempt to free up enough space
				// to store image
				final long freeDiskSpace = getFreeDiskSpace(context);
				if (freeDiskSpace > diskFileCacheConfiguration.getMaxSize()) return Promise.empty();

				return CacheFlusherTask
					.promisedCacheFlushing(context, diskFileCacheConfiguration.getCacheName(), freeDiskSpace + fileData.length)
					.eventually(v -> writeCachedFileWithRetries(cachedFileOutputStream, fileData));
			});

		return Promise.whenAny(cachedFileWritePromise, retryWritePromise);
	}

	public Promise<CachedFileOutputStream> promiseCachedFileOutputStream(final String uniqueKey) {
		return cachedFilesProvider
			.promiseCachedFile(uniqueKey)
			.then(cachedFile -> {
				final File file = cachedFile != null ? new File(cachedFile.getFileName()) : generateCacheFile(uniqueKey);

				return new CachedFileOutputStream(uniqueKey, file, diskFileCachePersistence);
			});
	}

	public Promise<File> promiseCachedFile(final String uniqueKey) {
		return cachedFilesProvider
			.promiseCachedFile(uniqueKey)
			.then(cachedFile -> {
				try {
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
					if (expirationTime > -1 && cachedFile.getCreatedTime() < System.currentTimeMillis() - expirationTime) {
						logger.info("Cached serviceFile " + uniqueKey + " expired. Deleting.");
						if (!returnFile.delete())
							throw new IOException("Unable to delete serviceFile " + returnFile.getAbsolutePath());

						if (deleteCachedFile(cachedFile.getId()) <= 0)
							throw new SQLDataException("Unable to delete serviceFile with ID " + cachedFile.getId());

						return null;
					}

					diskFileAccessTimeUpdater.promiseFileAccessedUpdate(cachedFile);

					logger.info("Returning cached serviceFile " + uniqueKey);
					return returnFile;
				} catch (SQLException sqlException) {
					logger.error("There was an error attempting to get the cached serviceFile " + uniqueKey, sqlException);
					return null;
				}
			});
	}

	public Promise<Boolean> containsKey(final String uniqueKey) throws IOException {
		return promiseCachedFile(uniqueKey).then(file -> file != null);
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
				logger.warn("There was an error trying to delete the cached serviceFile with id " + cachedFileId, sqlException);
			}
		}

		return -1;
	}

	private File generateCacheFile(String uniqueKey) {
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

		return file;
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
