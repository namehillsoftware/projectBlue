package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached;

import android.content.Context;
import android.database.SQLException;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.access.ICachedFilesProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.disk.IDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.persistence.IDiskFileAccessTimeUpdater;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.CacheOutputStream;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.cached.stream.supplier.ICacheStreamSupplier;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLDataException;

import okio.Buffer;

public class DiskFileCache implements ICache {

	private final static Logger logger = LoggerFactory.getLogger(DiskFileCache.class);

	private final Context context;
	private final IDiskCacheDirectoryProvider diskCacheDirectory;
	private final IDiskFileCacheConfiguration diskFileCacheConfiguration;
	private final ICacheStreamSupplier cacheStreamSupplier;
	private final ICachedFilesProvider cachedFilesProvider;
	private final IDiskFileAccessTimeUpdater diskFileAccessTimeUpdater;

	private final long expirationTime;

	public DiskFileCache(final Context context, IDiskCacheDirectoryProvider diskCacheDirectory, IDiskFileCacheConfiguration diskFileCacheConfiguration, ICacheStreamSupplier cacheStreamSupplier, ICachedFilesProvider cachedFilesProvider, IDiskFileAccessTimeUpdater diskFileAccessTimeUpdater) {
		this.context = context;
		this.diskCacheDirectory = diskCacheDirectory;
		this.diskFileCacheConfiguration = diskFileCacheConfiguration;

		expirationTime = diskFileCacheConfiguration.getCacheItemLifetime() != null
			? diskFileCacheConfiguration.getCacheItemLifetime().getMillis()
			: -1;

		this.cacheStreamSupplier = cacheStreamSupplier;
		this.cachedFilesProvider = cachedFilesProvider;
		this.diskFileAccessTimeUpdater = diskFileAccessTimeUpdater;
	}

	public Promise<CachedFile> put(final String uniqueKey, final byte[] fileData) {
		final Promise<CachedFile> putPromise =
			cacheStreamSupplier
				.promiseCachedFileOutputStream(uniqueKey)
				.eventually(cachedFileOutputStream -> writeCachedFileWithRetries(uniqueKey, cachedFileOutputStream, fileData));

		putPromise.excuse(e -> {
			logger.error("There was an error putting the cached file with the unique key " + uniqueKey + " into the cache.", e);

			return null;
		});

		return putPromise;
	}

	private Promise<CachedFile> writeCachedFileWithRetries(final String uniqueKey, CacheOutputStream cachedFileOutputStream, byte[] fileData) {
		return cachedFileOutputStream
			.promiseWrite(fileData, 0, fileData.length)
			.eventually(CacheOutputStream::flush)
			.eventually(fos -> {
				fos.close();
				return fos.commitToCache();
			}, e -> {
				logger.error("Unable to write to file!", e);

				cachedFileOutputStream.close();

				if (!(e instanceof IOException)) return Promise.empty();

				// Check if free space is too low and then attempt to free up enough space
				// to store image
				final long freeDiskSpace = getFreeDiskSpace();
				if (freeDiskSpace > diskFileCacheConfiguration.getMaxSize()) return Promise.empty();

				return CacheFlusherTask
					.promisedCacheFlushing(context, diskCacheDirectory, diskFileCacheConfiguration, freeDiskSpace + fileData.length)
					.eventually(v -> put(uniqueKey, fileData));
			});
	}

	public Promise<CachedFile> put(final String uniqueKey, final Buffer buffer) {
		final Promise<CachedFile> putPromise =
			cacheStreamSupplier
				.promiseCachedFileOutputStream(uniqueKey)
				.eventually(cachedFileOutputStream -> writeCachedFileWithRetries(cachedFileOutputStream, buffer));

		putPromise.excuse(e -> {
			logger.error("There was an error putting the cached file with the unique key " + uniqueKey + " into the cache.", e);

			return null;
		});

		return putPromise;
	}

	private Promise<CachedFile> writeCachedFileWithRetries(CacheOutputStream cachedFileOutputStream, Buffer buffer) {
		final long bufferSize = buffer.size();

		return cachedFileOutputStream
			.promiseTransfer(buffer)
			.eventually(CacheOutputStream::flush)
			.eventually(fos -> {
				fos.close();
				return fos.commitToCache();
			}, e -> {
				logger.error("Unable to write to file!", e);

				if (!(e instanceof IOException)) return Promise.empty();

				// Check if free space is too low and then attempt to free up enough space
				// to store image
				final long freeDiskSpace = getFreeDiskSpace();
				if (freeDiskSpace > diskFileCacheConfiguration.getMaxSize()) return Promise.empty();

				return CacheFlusherTask
					.promisedCacheFlushing(context, diskCacheDirectory, diskFileCacheConfiguration, freeDiskSpace + bufferSize)
					.eventually(v -> writeCachedFileWithRetries(cachedFileOutputStream, buffer));
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
						logger.warn("Cached file `" + cachedFile.getFileName() + "` doesn't exist! Removing from database.");
						if (deleteCachedFile(cachedFile.getId()) <= 0)
							throw new SQLDataException("Unable to delete serviceFile with ID " + cachedFile.getId());

						return null;
					}

					// Remove the serviceFile and return null if it's past its expired time
					if (expirationTime > -1 && cachedFile.getCreatedTime() < System.currentTimeMillis() - expirationTime) {
						logger.info("Cached file " + uniqueKey + " expired. Deleting.");
						if (!returnFile.delete())
							throw new IOException("Unable to delete cached file " + returnFile.getAbsolutePath());

						if (deleteCachedFile(cachedFile.getId()) <= 0)
							throw new SQLDataException("Unable to delete cached file with ID " + cachedFile.getId());

						return null;
					}

					diskFileAccessTimeUpdater.promiseFileAccessedUpdate(cachedFile);

					logger.info("Returning cached file " + uniqueKey);
					return returnFile;
				} catch (SQLException sqlException) {
					logger.error("There was an error attempting to get the cached file " + uniqueKey, sqlException);
					return null;
				}
			});
	}

	public Promise<Boolean> containsKey(final String uniqueKey) {
		return promiseCachedFile(uniqueKey).then(file -> file != null);
	}

	private long deleteCachedFile(final long cachedFileId) {
		try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
			try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
				logger.info("Deleting cached file with id " + cachedFileId);

				if (logger.isDebugEnabled())
					logger.debug("Cached file count: " + getTotalCachedFileCount(repositoryAccessHelper));

				final long executionResult =
						repositoryAccessHelper
								.mapSql("DELETE FROM " + CachedFile.tableName + " WHERE id = @id")
								.addParameter("id", cachedFileId)
								.execute();

				if (logger.isDebugEnabled())
					logger.debug("Cached file count: " + getTotalCachedFileCount(repositoryAccessHelper));

				closeableTransaction.setTransactionSuccessful();

				return executionResult;
			} catch (SQLException sqlException) {
				logger.warn("There was an error trying to delete the cached file with id " + cachedFileId, sqlException);
			}
		}

		return -1;
	}

	private static long getTotalCachedFileCount(RepositoryAccessHelper repositoryAccessHelper) {
		return repositoryAccessHelper.mapSql("SELECT COUNT(*) FROM " + CachedFile.tableName).execute();
	}

	private long getFreeDiskSpace() {
		return diskCacheDirectory.getDiskCacheDirectory(diskFileCacheConfiguration).getUsableSpace();
	}
}
