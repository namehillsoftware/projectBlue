package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.Lazy;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 7/14/15.
 */
public class StoredFileAccess {

	private static final Lazy<Logger> logger = new Lazy<>(new Callable<Logger>() {
		@Override
		public Logger call() throws Exception {
			return LoggerFactory.getLogger(StoredFileAccess.class);
		}
	});

	private static final Lazy<ExecutorService> storedFileExecutor = new Lazy<>(new Callable<ExecutorService>() {
		@Override
		public ExecutorService call() throws Exception {
			return Executors.newSingleThreadExecutor();
		}
	});

	private final Context context;
	private final Library library;

	private static final String selectFromStoredFiles = "SELECT * FROM " + StoredFile.tableName;

	public StoredFileAccess(Context context, Library library) {
		this.context = context;
		this.library = library;
	}

	public void getStoredFile(final int storedFileId, ITwoParameterRunnable<FluentTask<Void, Void, StoredFile>, StoredFile> onStoredFileRetrieved) {
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

		if (onStoredFileRetrieved != null)
			getStoredFileTask.onComplete(onStoredFileRetrieved);

		getStoredFileTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void getStoredFile(final IFile serviceFile, ITwoParameterRunnable<FluentTask<Void, Void, StoredFile>, StoredFile> onStoredFileRetrieved) {
		final FluentTask<Void, Void, StoredFile> getStoredFileTask = getStoredFileTask(serviceFile);

		if (onStoredFileRetrieved != null)
			getStoredFileTask.onComplete(onStoredFileRetrieved);

		getStoredFileTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public StoredFile getStoredFile(final IFile serviceFile) throws ExecutionException, InterruptedException {
		return getStoredFileTask(serviceFile).get(AsyncTask.THREAD_POOL_EXECUTOR);
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

	public void getDownloadingStoredFiles(ITwoParameterRunnable<FluentTask<Void, Void, List<StoredFile>>, List<StoredFile>> onGetDownloadingStoredFilesComplete) {
		final FluentTask<Void, Void, List<StoredFile>> getDownloadingStoredFilesTask = new FluentTask<Void, Void, List<StoredFile>>() {
			@Override
			protected List<StoredFile> executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					return repositoryAccessHelper
							.mapSql(
									selectFromStoredFiles + " WHERE " + StoredFile.isDownloadCompleteColumnName + " = :" + StoredFile.isDownloadCompleteColumnName)
							.addParameter(StoredFile.isDownloadCompleteColumnName, false)
							.fetch(StoredFile.class);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};

		if (onGetDownloadingStoredFilesComplete != null)
			getDownloadingStoredFilesTask.onComplete(onGetDownloadingStoredFilesComplete);

		getDownloadingStoredFilesTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void markStoredFileAsDownloaded(final int storedFileId) {
		storedFileExecutor.getObject().execute(new Runnable() {
			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					repositoryAccessHelper
							.mapSql(
									" UPDATE " + StoredFile.tableName +
											" SET " + StoredFile.isDownloadCompleteColumnName + " = 1" +
											" WHERE id = :id")
							.addParameter("id", storedFileId)
							.execute();
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	private void deleteStoredFile(final StoredFile storedFile) {
		storedFileExecutor.getObject().execute(new Runnable() {
			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Connection connection = repositoryAccessHelper.getConnection();
					try {
						connection
							.createQuery("DELETE FROM " + StoredFile.tableName + " WHERE id = :id")
							.addParameter("id", storedFile.getId())
							.executeScalar();
					} finally {
						connection.close();
					}
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	public void addMediaFile(final IFile file, final int mediaFileId, final String filePath) {
		storedFileExecutor.getObject().execute(new Runnable() {
			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					StoredFile storedFile = getStoredFile(repositoryAccessHelper, file);
					if (storedFile == null) {
						storedFile =
							repositoryAccessHelper
								.mapSql(selectFromStoredFiles + " WHERE " + StoredFile.storedMediaIdColumnName + " = :" + StoredFile.storedMediaIdColumnName)
								.addParameter(StoredFile.storedMediaIdColumnName, mediaFileId)
								.fetchFirst(StoredFile.class);

						if (storedFile != null && storedFile.getPath() != null && storedFile.getPath().equals(filePath)) return;
					}

					if (storedFile == null) {
						storedFile =
							repositoryAccessHelper
								.mapSql(selectFromStoredFiles + " WHERE " + StoredFile.pathColumnName + " = :" + StoredFile.pathColumnName)
									.addParameter(StoredFile.pathColumnName, filePath)
								.fetchFirst(StoredFile.class);
					}

					if (storedFile == null) {
						createStoredFile(repositoryAccessHelper, file);
						storedFile = getStoredFile(repositoryAccessHelper, file);
					}

					updateStoredFile(repositoryAccessHelper, storedFile);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	public StoredFile createOrUpdateFile(final IFile file) {
		final FluentTask<Void, Void, StoredFile> createOrUpdateStoredFileTask = new FluentTask<Void, Void, StoredFile>() {
			@Override
			public StoredFile executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					StoredFile storedFile = getStoredFile(repositoryAccessHelper, file);
					if (storedFile == null) {
						createStoredFile(repositoryAccessHelper, file);
						storedFile = getStoredFile(repositoryAccessHelper, file);
					}

					if (storedFile.getPath() == null) {
						try {
							final MediaFileUriProvider mediaFileUriProvider = new MediaFileUriProvider(context, file, true);
							final Uri localUri = mediaFileUriProvider.getFileUri();
							if (localUri != null) {
								storedFile.setPath(localUri.getPath());
								storedFile.setIsDownloadComplete(true);
								storedFile.setIsOwner(false);
								try {
									storedFile.setStoredMediaId(mediaFileUriProvider.getMediaId());
								} catch (IOException e) {
									logger.getObject().error("Error retrieving media file ID", e);
								}
							}
						} catch (IOException e) {
							logger.getObject().error("Error retrieving media file URI", e);
						}
					}

					if (storedFile.getPath() == null) {
						try {
							String fullPath = library.getSyncDir(context).getPath();

							String artist = file.tryGetProperty(FilePropertiesProvider.ALBUM_ARTIST);
							if (artist == null)
								artist = file.tryGetProperty(FilePropertiesProvider.ARTIST);

							if (artist != null)
								fullPath = FilenameUtils.concat(fullPath, artist);

							final String album = file.tryGetProperty(FilePropertiesProvider.ALBUM);
							if (album != null)
								fullPath = FilenameUtils.concat(fullPath, album);

							String fileName = file.getProperty(FilePropertiesProvider.FILENAME);
							fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);

							final int extensionIndex = fileName.lastIndexOf('.');
							if (extensionIndex > -1)
								fileName = fileName.substring(0, extensionIndex + 1) + "mp3";

							// The media player API apparently bombs on colons, so let's cleanse it of colons (tee-hee)
							fullPath = FilenameUtils.concat(fullPath, fileName).replace(':', '_');
							storedFile.setPath(fullPath);
						} catch (IOException e) {
							logger.getObject().error("Error getting filename for file " + file.getValue(), e);
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
			return createOrUpdateStoredFileTask.get(storedFileExecutor.getObject());
		} catch (ExecutionException | InterruptedException e) {
			logger.getObject().error("There was an error creating or updating the stored file for service file " + file.getKey(), e);
			return null;
		}
	}

	public void pruneStoredFiles(final Set<Integer> serviceIdsToKeep) {
		storedFileExecutor.getObject().execute(new Runnable() {
			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					// Since we could be pulling back a lot of data, only query for what we need.
					// This query is very custom to this scenario, so it's being kept here.
					final List<StoredFile> allStoredFiles =
							repositoryAccessHelper
								.mapSql(
									" SELECT id, " + StoredFile.serviceIdColumnName + ", " + StoredFile.pathColumnName +
									" FROM " + StoredFile.tableName +
									" WHERE " + StoredFile.libraryIdColumnName + " = :" + StoredFile.libraryIdColumnName +
									" AND " + StoredFile.isOwnerColumnName + " = :" + StoredFile.isOwnerColumnName)
								.addParameter(StoredFile.libraryIdColumnName, library.getId())
								.addParameter(StoredFile.isOwnerColumnName, true)
								.fetch(StoredFile.class);

					for (StoredFile storedFile : allStoredFiles) {
						if (!serviceIdsToKeep.contains(storedFile.getServiceId()))
							deleteStoredFile(storedFile);
					}
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	private StoredFile getStoredFile(RepositoryAccessHelper helper, IFile file) {
		return helper
				.mapSql(
						" SELECT * " +
								" FROM " + StoredFile.tableName + " " +
								" WHERE " + StoredFile.serviceIdColumnName + " = :" + StoredFile.serviceIdColumnName +
								" AND " + StoredFile.libraryIdColumnName + " = :" + StoredFile.libraryIdColumnName)
				.addParameter(StoredFile.serviceIdColumnName, file.getKey())
				.addParameter(StoredFile.libraryIdColumnName, library.getId())
				.fetchFirst(StoredFile.class);
	}

	private static StoredFile getStoredFile(RepositoryAccessHelper helper, int storedFileId) {
		return helper
				.mapSql("SELECT * FROM " + StoredFile.tableName + " WHERE id = :id")
				.addParameter("id", storedFileId)
				.fetchFirst(StoredFile.class);
	}

	private void createStoredFile(RepositoryAccessHelper repositoryAccessHelper, IFile file) {
		repositoryAccessHelper
				.mapSql(
						" INSERT INTO " + StoredFile.tableName + " (" +
								StoredFile.serviceIdColumnName + ", " +
								StoredFile.libraryIdColumnName + ", " +
								StoredFile.isDownloadCompleteColumnName + ") VALUES (" +
								":" + StoredFile.serviceIdColumnName + ", " +
								":" + StoredFile.libraryIdColumnName + ", " +
								"1)")
				.addParameter(StoredFile.serviceIdColumnName, file.getKey())
				.addParameter(StoredFile.libraryIdColumnName, library.getId())
				.execute();
	}

	private static void updateStoredFile(RepositoryAccessHelper repositoryAccessHelper, StoredFile storedFile) {
		final String updateSql =
				" UPDATE " + StoredFile.tableName +
				" SET " + StoredFile.pathColumnName + " = :" + StoredFile.pathColumnName +
				", " + StoredFile.storedMediaIdColumnName + " = :" + StoredFile.storedMediaIdColumnName +
				", " + StoredFile.isOwnerColumnName + " = :" + StoredFile.isOwnerColumnName +
				", " + StoredFile.isDownloadCompleteColumnName + " = :" + StoredFile.isDownloadCompleteColumnName +
				" WHERE id = :id";

		repositoryAccessHelper
				.mapSql(updateSql)
				.addParameter(StoredFile.storedMediaIdColumnName, storedFile.getStoredMediaId())
				.addParameter(StoredFile.pathColumnName, storedFile.getPath())
				.addParameter(StoredFile.isOwnerColumnName, storedFile.isOwner())
				.addParameter(StoredFile.isDownloadCompleteColumnName, storedFile.isDownloadComplete())
				.addParameter("id", storedFile.getId())
				.execute();
	}
}
