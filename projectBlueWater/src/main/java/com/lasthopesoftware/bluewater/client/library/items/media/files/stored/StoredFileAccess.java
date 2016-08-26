package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
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
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 7/14/15.
 */
public class StoredFileAccess {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileAccess.class);

	public static final ExecutorService storedFileExecutor = Executors.newSingleThreadExecutor();

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

	public void getStoredFile(final int storedFileId, TwoParameterRunnable<FluentTask<Void, Void, StoredFile>, StoredFile> onStoredFileRetrieved) {
		final FluentTask<Void, Void, StoredFile> getStoredFileTask = new FluentTask<Void, Void, StoredFile>() {
			@Override
			protected StoredFile executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					return getStoredFile(repositoryAccessHelper, storedFileId);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};

		getStoredFileTask.onComplete(onStoredFileRetrieved).execute(RepositoryAccessHelper.databaseExecutor);
	}

	public StoredFile getStoredFile(final IFile serviceFile) throws ExecutionException, InterruptedException {
		return getStoredFileTask(serviceFile).get(RepositoryAccessHelper.databaseExecutor);
	}

	private FluentTask<Void, Void, StoredFile> getStoredFileTask(final IFile serviceFile) {
		return new FluentTask<Void, Void, StoredFile>() {
			@Override
			public StoredFile executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					return getStoredFile(repositoryAccessHelper, serviceFile);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};
	}

	public void getDownloadingStoredFiles(TwoParameterRunnable<FluentTask<Void, Void, List<StoredFile>>, List<StoredFile>> onGetDownloadingStoredFilesComplete) {
		final FluentTask<Void, Void, List<StoredFile>> getDownloadingStoredFilesTask = new FluentTask<Void, Void, List<StoredFile>>() {
			@Override
			protected List<StoredFile> executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					return repositoryAccessHelper
							.mapSql(
									selectFromStoredFiles + " WHERE " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = @" + StoredFileEntityInformation.isDownloadCompleteColumnName)
							.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, false)
							.fetch(StoredFile.class);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};

		getDownloadingStoredFilesTask.onComplete(onGetDownloadingStoredFilesComplete).execute(RepositoryAccessHelper.databaseExecutor);
	}

	public void markStoredFileAsDownloaded(final StoredFile storedFile) {
		RepositoryAccessHelper.databaseExecutor.execute(() -> {
			final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
			try {
				repositoryAccessHelper
						.mapSql(
								" UPDATE " + StoredFileEntityInformation.tableName +
								" SET " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = 1" +
								" WHERE id = @id")
						.addParameter("id", storedFile.getId())
						.execute();
			} finally {
				repositoryAccessHelper.close();
			}

			storedFile.setIsDownloadComplete(true);
		});
	}

	public void addMediaFile(final IFile file, final int mediaFileId, final String filePath) {
		RepositoryAccessHelper.databaseExecutor.execute(() -> {
			final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
			try {
				StoredFile storedFile = getStoredFile(repositoryAccessHelper, file);
				if (storedFile == null) {
					storedFile =
						repositoryAccessHelper
							.mapSql(selectFromStoredFiles + " WHERE " + StoredFileEntityInformation.storedMediaIdColumnName + " = @" + StoredFileEntityInformation.storedMediaIdColumnName)
							.addParameter(StoredFileEntityInformation.storedMediaIdColumnName, mediaFileId)
							.fetchFirst(StoredFile.class);

					if (storedFile != null && storedFile.getPath() != null && storedFile.getPath().equals(filePath)) return;
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
			} finally {
				repositoryAccessHelper.close();
			}
		});
	}

	public StoredFile createOrUpdateFile(IConnectionProvider connectionProvider, final IFile file) {
		final FluentTask<Void, Void, StoredFile> createOrUpdateStoredFileTask = new FluentTask<Void, Void, StoredFile>() {
			@Override
			public StoredFile executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					StoredFile storedFile = getStoredFile(repositoryAccessHelper, file);
					if (storedFile == null) {
						logger.info("Stored file was not found for " + file.getKey() + ", creating file");
						createStoredFile(repositoryAccessHelper, file);
						storedFile = getStoredFile(repositoryAccessHelper, file);
					}

					if (storedFile.getPath() == null && library.isUsingExistingFiles()) {
						try {
							final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator = new ExternalStorageReadPermissionsArbitratorForOs(context);
							final IMediaQueryCursorProvider mediaQueryCursorProvider = new MediaQueryCursorProvider(context, connectionProvider);

							final MediaFileUriProvider mediaFileUriProvider =
									new MediaFileUriProvider(context, mediaQueryCursorProvider, file, externalStorageReadPermissionsArbitrator, true);

							final Uri localUri = mediaFileUriProvider.getFileUri();
							if (localUri != null) {
								storedFile.setPath(localUri.getPath());
								storedFile.setIsDownloadComplete(true);
								storedFile.setIsOwner(false);
								try {
									final MediaFileIdProvider mediaFileIdProvider = new MediaFileIdProvider(mediaQueryCursorProvider, file, externalStorageReadPermissionsArbitrator);
									storedFile.setStoredMediaId(mediaFileIdProvider.getMediaId());
								} catch (IOException e) {
									logger.error("Error retrieving media file ID", e);
								}
							}
						} catch (IOException e) {
							logger.error("Error retrieving media file URI", e);
						}
					}

					if (storedFile.getPath() == null) {
						try {
							String fullPath = library.getSyncDir(context).getPath();

							final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, file.getKey());
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
				} finally {
					repositoryAccessHelper.close();
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
			new PruneFilesTask(context, library.getId(), serviceIdsToKeep).get(storedFileExecutor);
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

	private void createStoredFile(RepositoryAccessHelper repositoryAccessHelper, IFile file) {
		final CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction();
		try {
			repositoryAccessHelper
					.mapSql(insertSql.getObject())
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, file.getKey())
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
					.execute();

			closeableTransaction.setTransactionSuccessful();
		} finally {
			closeableTransaction.close();
		}
	}

	private static void updateStoredFile(RepositoryAccessHelper repositoryAccessHelper, StoredFile storedFile) {
		final CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction();
		try {
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
		} finally {
			closeableTransaction.close();
		}
	}
}
