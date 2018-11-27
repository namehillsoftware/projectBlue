package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence;

import android.content.Context;
import android.database.SQLException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.CacheFlusherTask;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access.ICachedFilesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.IDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.artful.Artful;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.lazyj.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class DiskFileCachePersistence implements IDiskFileCachePersistence {
	private final static Logger logger = LoggerFactory.getLogger(DiskFileCachePersistence.class);

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

	private final Context context;
	private final ICachedFilesProvider cachedFilesProvider;
	private final IDiskFileAccessTimeUpdater diskFileAccessTimeUpdater;
	private final IDiskCacheDirectoryProvider diskCacheDirectoryProvider;
	private final IDiskFileCacheConfiguration diskFileCacheConfiguration;

	public DiskFileCachePersistence(Context context, IDiskCacheDirectoryProvider diskCacheDirectoryProvider, IDiskFileCacheConfiguration diskFileCacheConfiguration, ICachedFilesProvider cachedFilesProvider, IDiskFileAccessTimeUpdater diskFileAccessTimeUpdater) {
		this.context = context;
		this.diskCacheDirectoryProvider = diskCacheDirectoryProvider;
		this.diskFileCacheConfiguration = diskFileCacheConfiguration;
		this.cachedFilesProvider = cachedFilesProvider;
		this.diskFileAccessTimeUpdater = diskFileAccessTimeUpdater;
	}

	@Override
	public Promise<CachedFile> putIntoDatabase(String uniqueKey, File file) {
		final String canonicalFilePath;
		try {
			canonicalFilePath = file.getCanonicalPath();
		} catch (IOException e) {
			logger.error("There was an error getting the canonical path for " + file, e);
			return Promise.empty();
		}

		return cachedFilesProvider
			.promiseCachedFile(uniqueKey)
			.eventually(cachedFile -> {
				if (cachedFile != null) {
					return !cachedFile.getFileName().equals(canonicalFilePath)
						? promiseFilePathUpdate(cachedFile).eventually(diskFileAccessTimeUpdater::promiseFileAccessedUpdate)
						: diskFileAccessTimeUpdater.promiseFileAccessedUpdate(cachedFile);
				}

				return new QueuedPromise<>(() -> {
					logger.info("File with unique key " + uniqueKey + " doesn't exist. Creating...");
					try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
						final Artful sqlInsertMapper = repositoryAccessHelper.mapSql(cachedFileSqlInsert.getObject());

						sqlInsertMapper.addParameter(CachedFile.FILE_NAME, canonicalFilePath);

						try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
							final long currentTimeMillis = System.currentTimeMillis();
							sqlInsertMapper
								.addParameter(CachedFile.CACHE_NAME, diskFileCacheConfiguration.getCacheName())
								.addParameter(CachedFile.FILE_SIZE, file.length())
								.addParameter(CachedFile.LIBRARY_ID, diskFileCacheConfiguration.getLibrary().getId())
								.addParameter(CachedFile.UNIQUE_KEY, uniqueKey)
								.addParameter(CachedFile.CREATED_TIME, currentTimeMillis)
								.addParameter(CachedFile.LAST_ACCESSED_TIME, currentTimeMillis)
								.execute();

							closeableTransaction.setTransactionSuccessful();
						} catch (SQLException sqlException) {
							logger.warn("There was an error inserting the cached serviceFile with the unique key " + uniqueKey, sqlException);
							throw sqlException;
						}
					} finally {
						CacheFlusherTask.promisedCacheFlushing(context, diskCacheDirectoryProvider, diskFileCacheConfiguration, diskFileCacheConfiguration.getMaxSize());
					}

					return null;
				}, RepositoryAccessHelper.databaseExecutor)
				.eventually(v -> cachedFilesProvider.promiseCachedFile(uniqueKey));
			});
	}

	private Promise<CachedFile> promiseFilePathUpdate(final CachedFile cachedFile) {
		return new QueuedPromise<>(() -> {
			final long cachedFileId = cachedFile.getId();
			final String cachedFilePath = cachedFile.getFileName();

			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
					logger.info("Updating serviceFile name of cached serviceFile with ID " + cachedFileId + " to " + cachedFilePath);

					repositoryAccessHelper
						.mapSql("UPDATE " + CachedFile.tableName + " SET " + CachedFile.FILE_NAME + " = @" + CachedFile.FILE_NAME + " WHERE id = @id")
						.addParameter(CachedFile.FILE_NAME, cachedFilePath)
						.addParameter("id", cachedFileId)
						.execute();

					closeableTransaction.setTransactionSuccessful();
				} catch (SQLException sqlException) {
					logger.error("There was an error trying to update the cached serviceFile with ID " + cachedFileId, sqlException);
					throw sqlException;
				}
			}

			return cachedFile;
		}, RepositoryAccessHelper.databaseExecutor);
	}
}
