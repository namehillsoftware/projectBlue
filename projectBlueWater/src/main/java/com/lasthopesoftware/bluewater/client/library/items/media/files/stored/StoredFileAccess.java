package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.SQLException;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.IMediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.repository.UpdateBuilder;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.vedsoft.fluent.FluentCallable;
import com.vedsoft.fluent.FluentSpecifiedTask;
import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.lazyj.Lazy;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

/**
 * Created by david on 7/14/15.
 */
public class StoredFileAccess {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileAccess.class);

	private final Context context;
	private final Library library;

	private static final String selectFromStoredFiles = "SELECT * FROM " + StoredFileEntityInformation.tableName;

	private static final Lazy<String> insertSql
			= new Lazy<>(() ->
				InsertBuilder
						.fromTable(StoredFileEntityInformation.tableName)
						.addColumn(StoredFileEntityInformation.serviceIdColumnName)
						.addColumn(StoredFileEntityInformation.libraryIdColumnName)
						.addColumn(StoredFileEntityInformation.isOwnerColumnName)
						.build());

	private static final Lazy<String> updateSql =
			new Lazy<>(() ->
					UpdateBuilder
							.fromTable(StoredFileEntityInformation.tableName)
							.addSetter(StoredFileEntityInformation.serviceIdColumnName)
							.addSetter(StoredFileEntityInformation.storedMediaIdColumnName)
							.addSetter(StoredFileEntityInformation.pathColumnName)
							.addSetter(StoredFileEntityInformation.isOwnerColumnName)
							.addSetter(StoredFileEntityInformation.isDownloadCompleteColumnName)
							.setFilter("WHERE id = @id")
							.buildQuery());

	public StoredFileAccess(Context context, Library library) {
		this.context = context;
		this.library = library;
	}

	@SuppressLint("NewApi")
	public FluentCallable<StoredFile> getStoredFile(final int storedFileId) {
		final FluentCallable<StoredFile> getStoredFileTask = new FluentCallable<StoredFile>() {
			@Override
			protected StoredFile executeInBackground() {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					return getStoredFile(repositoryAccessHelper, storedFileId);
				}
			}
		};

		getStoredFileTask.execute(RepositoryAccessHelper.databaseExecutor);

		return getStoredFileTask;
	}

	public StoredFile getStoredFile(final IFile serviceFile) throws ExecutionException, InterruptedException {
		return getStoredFileTask(serviceFile).get(RepositoryAccessHelper.databaseExecutor);
	}

	List<StoredFile> getAllStoredFilesInLibrary() throws ExecutionException, InterruptedException {
		return new FluentSpecifiedTask<Void, Void, List<StoredFile>>() {
			@Override
			@SuppressLint("NewApi")
			public List<StoredFile> executeInBackground(Void... params) {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					return repositoryAccessHelper
						.mapSql("SELECT * FROM " + StoredFileEntityInformation.tableName + " WHERE " + StoredFileEntityInformation.libraryIdColumnName + " + @" + StoredFileEntityInformation.libraryIdColumnName)
						.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
						.fetch(StoredFile.class);
				}
			}
		}.get(RepositoryAccessHelper.databaseExecutor);
	}

	@SuppressLint("NewApi")
	private IFluentTask<Void,Void,com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile> getStoredFileTask(final IFile serviceFile) {
		return new FluentSpecifiedTask<Void, Void, StoredFile>() {
			@Override
			public StoredFile executeInBackground(Void... params) {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					return getStoredFile(repositoryAccessHelper, serviceFile);
				}
			}
		};
	}

	@SuppressLint("NewApi")
	public FluentCallable<List<StoredFile>> getDownloadingStoredFiles() {
		final FluentCallable<List<StoredFile>> getDownloadingStoredFilesTask = new FluentCallable<List<StoredFile>>() {
			@Override
			protected List<StoredFile> executeInBackground() {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					return repositoryAccessHelper
							.mapSql(
									selectFromStoredFiles + " WHERE " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = @" + StoredFileEntityInformation.isDownloadCompleteColumnName)
							.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, false)
							.fetch(StoredFile.class);
				}
			}
		};

		getDownloadingStoredFilesTask.execute(RepositoryAccessHelper.databaseExecutor);

		return getDownloadingStoredFilesTask;
	}

	@SuppressLint("NewApi")
	public void markStoredFileAsDownloaded(final StoredFile storedFile) {
		RepositoryAccessHelper.databaseExecutor.execute(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {

					repositoryAccessHelper
							.mapSql(
									" UPDATE " + StoredFileEntityInformation.tableName +
											" SET " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = 1" +
											" WHERE id = @id")
							.addParameter("id", storedFile.getId())
							.execute();

					closeableTransaction.setTransactionSuccessful();
				}
			}

			storedFile.setIsDownloadComplete(true);
		});
	}

	@SuppressLint("NewApi")
	public void addMediaFile(final IFile file, final int mediaFileId, final String filePath) {
		RepositoryAccessHelper.databaseExecutor.execute(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				StoredFile storedFile = getStoredFile(repositoryAccessHelper, file);
				if (storedFile == null) {
					storedFile =
							repositoryAccessHelper
									.mapSql(selectFromStoredFiles + " WHERE " + StoredFileEntityInformation.storedMediaIdColumnName + " = @" + StoredFileEntityInformation.storedMediaIdColumnName)
									.addParameter(StoredFileEntityInformation.storedMediaIdColumnName, mediaFileId)
									.fetchFirst(StoredFile.class);

					if (storedFile != null && storedFile.getPath() != null && storedFile.getPath().equals(filePath))
						return;
				}

				if (storedFile == null) {
					storedFile =
							repositoryAccessHelper
									.mapSql(selectFromStoredFiles + " WHERE " + StoredFileEntityInformation.pathColumnName + " = @" + StoredFileEntityInformation.pathColumnName)
									.addParameter(StoredFileEntityInformation.pathColumnName, filePath)
									.fetchFirst(StoredFile.class);
				}

				if (storedFile == null) {
					createStoredFile(repositoryAccessHelper, file);
					storedFile = getStoredFile(repositoryAccessHelper, file);
					storedFile.setIsOwner(false);
					storedFile.setIsDownloadComplete(true);
				}

				storedFile.setStoredMediaId(mediaFileId);
				updateStoredFile(repositoryAccessHelper, storedFile);
			}
		});
	}

	public StoredFile createOrUpdateFile(IConnectionProvider connectionProvider, final IFile file) {
		final FluentCallable<StoredFile> createOrUpdateStoredFileTask = new FluentCallable<StoredFile>() {

			@Override
			@SuppressLint("NewApi")
			public StoredFile executeInBackground() {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					StoredFile storedFile = getStoredFile(repositoryAccessHelper, file);
					if (storedFile == null) {
						logger.info("Stored file was not found for " + file.getKey() + ", creating file");
						createStoredFile(repositoryAccessHelper, file);
						storedFile = getStoredFile(repositoryAccessHelper, file);
					}

					final IFilePropertiesContainerRepository filePropertiesContainerRepository = FilePropertyCache.getInstance();
					final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertiesContainerRepository, new FilePropertiesProvider(connectionProvider, filePropertiesContainerRepository));

					if (storedFile.getPath() == null && library.isUsingExistingFiles()) {
						final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator = new ExternalStorageReadPermissionsArbitratorForOs(context);
						final IMediaQueryCursorProvider mediaQueryCursorProvider = new MediaQueryCursorProvider(context, cachedFilePropertiesProvider);

						final MediaFileUriProvider mediaFileUriProvider =
								new MediaFileUriProvider(context, mediaQueryCursorProvider, externalStorageReadPermissionsArbitrator, library, true);

						mediaFileUriProvider.getFileUri(file)
							.then(localUri -> {
								if (localUri != null) {
									storedFile.setPath(localUri.getPath());
									storedFile.setIsDownloadComplete(true);
									storedFile.setIsOwner(false);
									try {
										final MediaFileIdProvider mediaFileIdProvider = new MediaFileIdProvider(mediaQueryCursorProvider, file, externalStorageReadPermissionsArbitrator);
										mediaFileIdProvider
											.getMediaId()
											.then(runCarelessly(mediaId -> storedFile.setStoredMediaId(mediaId)));
									} catch (IOException e) {
										logger.error("Error retrieving media file ID", e);
									}
								}
							})
							.error(runCarelessly(e -> logger.error("Error retrieving media file URI", e)));
					}

					if (storedFile.getPath() == null) {
						try {
							String fullPath = library.getSyncDir(context).getPath();

							final Map<String, String> fileProperties = filePropertiesProvider.get();

							String artist = fileProperties.get(FilePropertiesProvider.ALBUM_ARTIST);
							if (artist == null)
								artist = fileProperties.get(FilePropertiesProvider.ARTIST);

							if (artist != null)
								fullPath = FilenameUtils.concat(fullPath, artist);

							final String album = fileProperties.get(FilePropertiesProvider.ALBUM);
							if (album != null)
								fullPath = FilenameUtils.concat(fullPath, album);

							String fileName = fileProperties.get(FilePropertiesProvider.FILENAME);
							fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);

							final int extensionIndex = fileName.lastIndexOf('.');
							if (extensionIndex > -1)
								fileName = fileName.substring(0, extensionIndex + 1) + "mp3";

							// The media player library apparently bombs on colons, so let's cleanse it of colons (tee-hee)
							fullPath = FilenameUtils.concat(fullPath, fileName).replace(':', '_');
							storedFile.setPath(fullPath);
						} catch (InterruptedException | ExecutionException e) {
							logger.error("Error getting file properties for file " + file.getKey(), e);
						}
					}

					final File systemFile = new File(storedFile.getPath());
					if (!systemFile.exists())
						storedFile.setIsDownloadComplete(false);

					updateStoredFile(repositoryAccessHelper, storedFile);

					return storedFile;
				}
			}
		};

		try {
			return createOrUpdateStoredFileTask.get(RepositoryAccessHelper.databaseExecutor);
		} catch (ExecutionException | InterruptedException e) {
			logger.error("There was an error creating or updating the stored file for service file " + file.getKey(), e);
			return null;
		}
	}

	public void pruneStoredFiles(final Set<Integer> serviceIdsToKeep) {
		try {
			new PruneFilesTask(context, library, serviceIdsToKeep).get();
		} catch (ExecutionException | InterruptedException e) {
			logger.error("There was an exception while pruning the files", e);
		}
	}

	private StoredFile getStoredFile(RepositoryAccessHelper helper, IFile file) {
		return
			helper
				.mapSql(
					" SELECT * " +
					" FROM " + StoredFileEntityInformation.tableName + " " +
					" WHERE " + StoredFileEntityInformation.serviceIdColumnName + " = @" + StoredFileEntityInformation.serviceIdColumnName +
					" AND " + StoredFileEntityInformation.libraryIdColumnName + " = @" + StoredFileEntityInformation.libraryIdColumnName)
				.addParameter(StoredFileEntityInformation.serviceIdColumnName, file.getKey())
				.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
				.fetchFirst(StoredFile.class);
	}

	private StoredFile getStoredFile(RepositoryAccessHelper helper, int storedFileId) {
		return
			helper
				.mapSql("SELECT * FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
				.addParameter("id", storedFileId)
				.fetchFirst(StoredFile.class);
	}

	@SuppressLint("NewApi")
	private void createStoredFile(RepositoryAccessHelper repositoryAccessHelper, IFile file) {
		try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			repositoryAccessHelper
					.mapSql(insertSql.getObject())
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, file.getKey())
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
					.execute();

			closeableTransaction.setTransactionSuccessful();
		}
	}

	@SuppressLint("NewApi")
	private static void updateStoredFile(RepositoryAccessHelper repositoryAccessHelper, StoredFile storedFile) {
		try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			repositoryAccessHelper
					.mapSql(updateSql.getObject())
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, storedFile.getServiceId())
					.addParameter(StoredFileEntityInformation.storedMediaIdColumnName, storedFile.getStoredMediaId())
					.addParameter(StoredFileEntityInformation.pathColumnName, storedFile.getPath())
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, storedFile.isOwner())
					.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, storedFile.isDownloadComplete())
					.addParameter("id", storedFile.getId())
					.execute();

			closeableTransaction.setTransactionSuccessful();
		}
	}


	void deleteStoredFile(final StoredFile storedFile) {
		RepositoryAccessHelper.databaseExecutor.execute(() -> {
			final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
			final CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction();
			try {
				repositoryAccessHelper
						.mapSql("DELETE FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
						.addParameter("id", storedFile.getId())
						.execute();

				closeableTransaction.setTransactionSuccessful();
			} catch (SQLException e) {
				logger.error("There was an error deleting file " + storedFile.getId(), e);
			} finally {
				closeableTransaction.close();
				repositoryAccessHelper.close();
			}
		});
	}
}
