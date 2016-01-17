package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored;

import android.content.Context;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;
import com.vedsoft.lazyj.Lazy;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 7/14/15.
 */
public class StoredFileAccess {

	private static final Lazy<Logger> logger = new Lazy<Logger>() {
		@Override
		protected Logger initialize() {
			return LoggerFactory.getLogger(StoredFileAccess.class);
		}
	};

	private static final Lazy<ExecutorService> storedFileExecutor = new Lazy<ExecutorService>() {
		@Override
		protected ExecutorService initialize() {
			return Executors.newSingleThreadExecutor();
		}
	};

	private final Context context;
	private final Library library;

	private static final String selectFromStoredFiles = "SELECT * FROM " + StoredFile.tableName;

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

		getStoredFileTask.onComplete(onStoredFileRetrieved).execute(AsyncTask.THREAD_POOL_EXECUTOR);
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

	public void getDownloadingStoredFiles(TwoParameterRunnable<FluentTask<Void, Void, List<StoredFile>>, List<StoredFile>> onGetDownloadingStoredFilesComplete) {
		final FluentTask<Void, Void, List<StoredFile>> getDownloadingStoredFilesTask = new FluentTask<Void, Void, List<StoredFile>>() {
			@Override
			protected List<StoredFile> executeInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					return repositoryAccessHelper
							.mapSql(
									selectFromStoredFiles + " WHERE " + StoredFile.isDownloadCompleteColumnName + " = @" + StoredFile.isDownloadCompleteColumnName)
							.addParameter(StoredFile.isDownloadCompleteColumnName, false)
							.fetch(StoredFile.class);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};

		getDownloadingStoredFilesTask.onComplete(onGetDownloadingStoredFilesComplete).execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void markStoredFileAsDownloaded(final StoredFile storedFile) {
		storedFileExecutor.getObject().execute(() -> {
			final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
			try {
				repositoryAccessHelper
						.mapSql(
								" UPDATE " + StoredFile.tableName +
										" SET " + StoredFile.isDownloadCompleteColumnName + " = 1" +
										" WHERE id = @id")
						.addParameter("id", storedFile.getId())
						.execute();
			} finally {
				repositoryAccessHelper.close();
			}

			storedFile.setIsDownloadComplete(true);
		});
	}

	private void deleteStoredFile(final StoredFile storedFile) {
		storedFileExecutor.getObject().execute(() -> {
			final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
			try {
				repositoryAccessHelper
					.mapSql("DELETE FROM " + StoredFile.tableName + " WHERE id = @id")
					.addParameter("id", storedFile.getId())
					.execute();
			} finally {
				repositoryAccessHelper.close();
			}
		});
	}

	public void addMediaFile(final IFile file, final int mediaFileId, final String filePath) {
		storedFileExecutor.getObject().execute(() -> {
			final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
			try {
				StoredFile storedFile = getStoredFile(repositoryAccessHelper, file);
				if (storedFile == null) {
					storedFile =
						repositoryAccessHelper
							.mapSql(selectFromStoredFiles + " WHERE " + StoredFile.storedMediaIdColumnName + " = @" + StoredFile.storedMediaIdColumnName)
							.addParameter(StoredFile.storedMediaIdColumnName, mediaFileId)
							.fetchFirst(StoredFile.class);

					if (storedFile != null && storedFile.getPath() != null && storedFile.getPath().equals(filePath)) return;
				}

				if (storedFile == null) {
					storedFile =
						repositoryAccessHelper
							.mapSql(selectFromStoredFiles + " WHERE " + StoredFile.pathColumnName + " = @" + StoredFile.pathColumnName)
								.addParameter(StoredFile.pathColumnName, filePath)
							.fetchFirst(StoredFile.class);
				}

				if (storedFile == null) {
					createStoredFile(repositoryAccessHelper, file);
					storedFile = getStoredFile(repositoryAccessHelper, file);
					storedFile.setIsOwner(false);
				}

				storedFile.setStoredMediaId(mediaFileId);
				updateStoredFile(repositoryAccessHelper, storedFile);
			} finally {
				repositoryAccessHelper.close();
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

							// The media player library apparently bombs on colons, so let's cleanse it of colons (tee-hee)
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
		AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
			final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
			try {
				final List<StoredFile> allStoredFilesQuery =
						repositoryAccessHelper
								.mapSql(" SELECT * FROM " + StoredFile.tableName)
								.fetch(StoredFile.class);

				final int libraryId = library.getId();

				for (StoredFile storedFile : allStoredFilesQuery) {
					if (deleteIfNotExists(storedFile) == null) continue;
					if (!storedFile.isOwner()) continue;
					if (storedFile.getLibraryId() != libraryId) continue;
					if (serviceIdsToKeep.contains(storedFile.getServiceId())) continue;

					try {
						deleteStoredFile(storedFile);
					} catch (SQLException e) {
						logger.getObject().error("There was an error deleting file " + storedFile.getId(), e);
					}

					final File systemFile = new File(storedFile.getPath());
					systemFile.delete();
				}
			} catch (SQLException e) {
				logger.getObject().error("There was an error getting the stored files", e);
			} finally {
				repositoryAccessHelper.close();
			}
		});
	}

	private StoredFile getStoredFile(RepositoryAccessHelper helper, IFile file) {
		return
			helper
				.mapSql(
					" SELECT * " +
					" FROM " + StoredFile.tableName + " " +
					" WHERE " + StoredFile.serviceIdColumnName + " = @" + StoredFile.serviceIdColumnName +
					" AND " + StoredFile.libraryIdColumnName + " = @" + StoredFile.libraryIdColumnName)
				.addParameter(StoredFile.serviceIdColumnName, file.getKey())
				.addParameter(StoredFile.libraryIdColumnName, library.getId())
				.fetchFirst(StoredFile.class);
	}

	private StoredFile getStoredFile(RepositoryAccessHelper helper, int storedFileId) {
		return
			helper
				.mapSql("SELECT * FROM " + StoredFile.tableName + " WHERE id = @id")
				.addParameter("id", storedFileId)
				.fetchFirst(StoredFile.class);
	}

	public StoredFile deleteIfNotExists(StoredFile storedFile) {
		final File systemFile = new File(storedFile.getPath());
		if (systemFile.exists()) return storedFile;

		deleteStoredFile(storedFile);
		return null;
	}

	private void createStoredFile(RepositoryAccessHelper repositoryAccessHelper, IFile file) {
		repositoryAccessHelper
				.mapSql(
						" INSERT INTO " + StoredFile.tableName + " (" +
								StoredFile.serviceIdColumnName + ", " +
								StoredFile.libraryIdColumnName + ", " +
								StoredFile.isOwnerColumnName + ") VALUES (" +
								"@" + StoredFile.serviceIdColumnName + ", " +
								"@" + StoredFile.libraryIdColumnName + ", " +
								"1)")
				.addParameter(StoredFile.serviceIdColumnName, file.getKey())
				.addParameter(StoredFile.libraryIdColumnName, library.getId())
				.execute();
	}

	private static void updateStoredFile(RepositoryAccessHelper repositoryAccessHelper, StoredFile storedFile) {
		final String updateSql =
				" UPDATE " + StoredFile.tableName +
				" SET " + StoredFile.pathColumnName + " = @" + StoredFile.pathColumnName +
				", " + StoredFile.storedMediaIdColumnName + " = @" + StoredFile.storedMediaIdColumnName +
				", " + StoredFile.isOwnerColumnName + " = @" + StoredFile.isOwnerColumnName +
				", " + StoredFile.isDownloadCompleteColumnName + " = @" + StoredFile.isDownloadCompleteColumnName +
				" WHERE id = @id";

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
