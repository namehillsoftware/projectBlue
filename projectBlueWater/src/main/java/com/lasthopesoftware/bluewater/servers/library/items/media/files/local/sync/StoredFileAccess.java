package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 7/14/15.
 */
public class StoredFileAccess {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileAccess.class);
	private static final ExecutorService storedFileExecutor = Executors.newSingleThreadExecutor();

	private final Context context;
	private final Library library;

	public StoredFileAccess(Context context, Library library) {
		this.context = context;
		this.library = library;
	}

	public void getStoredFile(final int storedFileId, ITwoParameterRunnable<FluentTask<Void, Void, StoredFile>, StoredFile> onStoredFileRetrieved) {
		final FluentTask<Void, Void, StoredFile> getStoredFileTask = new FluentTask<Void, Void, StoredFile>() {
			@Override
			protected StoredFile doInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = repositoryAccessHelper.getDataAccess(StoredFile.class);
					return storedFileAccess.queryForId(storedFileId);
				} catch (SQLException se) {
					logger.error("There was an error retrieving the stored file", se);
					return null;
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
		return getStoredFileTask(serviceFile).execute(AsyncTask.THREAD_POOL_EXECUTOR).get();
	}

	private FluentTask<Void, Void, StoredFile> getStoredFileTask(final IFile serviceFile) {
		return new FluentTask<Void, Void, StoredFile>() {
			@Override
			public StoredFile doInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = repositoryAccessHelper.getDataAccess(StoredFile.class);
					return getStoredFile(storedFileAccess, serviceFile);
				} catch (SQLException se) {
					logger.error("There was an error retrieving the stored file", se);
					return null;
				} finally {
					repositoryAccessHelper.close();
				}
			}
		};
	}

	public void getDownloadingStoredFiles(ITwoParameterRunnable<FluentTask<Void, Void, List<StoredFile>>, List<StoredFile>> onGetDownloadingStoredFilesComplete) {
		final FluentTask<Void, Void, List<StoredFile>> getDownloadingStoredFilesTask = new FluentTask<Void, Void, List<StoredFile>>() {
			@Override
			protected List<StoredFile> doInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = repositoryAccessHelper.getDataAccess(StoredFile.class);
					return storedFileAccess.queryForEq(StoredFile.isDownloadCompleteColumnName, false);
				} catch (SQLException se) {
					logger.error("There was an error retrieving the downloading files.", se);
					return new ArrayList<>();
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
		storedFileExecutor.execute(new Runnable() {
			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = repositoryAccessHelper.getDataAccess(StoredFile.class);
					final StoredFile storedFile = storedFileAccess.queryForId(storedFileId);

					storedFile.setIsDownloadComplete(true);
					try {
						storedFileAccess.createOrUpdate(storedFile);
					} catch (SQLException se) {
						logger.error("There was an error updating the stored file", se);
					}
				} catch (SQLException se) {
					logger.error("There was an error retrieving the stored file", se);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	private void deleteStoredFile(final StoredFile storedFile) {
		storedFileExecutor.execute(new Runnable() {
			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = repositoryAccessHelper.getDataAccess(StoredFile.class);
					try {
						storedFileAccess.delete(storedFile);

						if (!storedFile.isOwner()) return;

						final File file = new File(storedFile.getPath());
						if (file.exists()) file.delete();
					} catch (SQLException se) {
						logger.error("There was an error updating the stored file", se);
					}
				} catch (SQLException se) {
					logger.error("There was an error retrieving the stored file", se);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	public void addMediaFile(final IFile file, final int mediaFileId, final String filePath) {
		storedFileExecutor.execute(new Runnable() {
			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = repositoryAccessHelper.getDataAccess(StoredFile.class);
					StoredFile storedFile = getStoredFile(storedFileAccess, file);
					if (storedFile == null) {
						final List<StoredFile> storedFiles = storedFileAccess.queryForEq(StoredFile.storedMediaIdColumnName, mediaFileId);
						if (storedFiles.size() > 0)
							storedFile = storedFiles.get(0);

						if (storedFile != null && storedFile.getPath() != null && storedFile.getPath().equals(filePath))
							return;
					}

					if (storedFile == null) {
						final List<StoredFile> storedFiles = storedFileAccess.queryForEq(StoredFile.pathColumnName, filePath);
						if (storedFiles.size() > 0)
							storedFile = storedFiles.get(0);
					}

					if (storedFile == null) {
						storedFile = new StoredFile();
						storedFile.setServiceId(file.getKey());
						storedFile.setLibraryId(library.getId());
						storedFile.setIsOwner(true);
					}

					storedFile.setStoredMediaId(mediaFileId);
					storedFile.setPath(filePath);

					try {
						storedFileAccess.createOrUpdate(storedFile);
					} catch (SQLException se) {
						logger.error("There was an updating/creating the stored file", se);
					}
				} catch (SQLException se) {
					logger.error("There was an error retrieving the stored file", se);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	public StoredFile createOrUpdateFile(final IFile file) {
		final FluentTask<Void, Void, StoredFile> createOrUpdateStoredFileTask = new FluentTask<Void, Void, StoredFile>() {
			@Override
			public StoredFile doInBackground(Void... params) {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<StoredFile, Integer> storedFilesAccess = repositoryAccessHelper.getDataAccess(StoredFile.class);
					StoredFile storedFile = getStoredFile(storedFilesAccess, file);
					if (storedFile == null) {
						storedFile = new StoredFile();
						storedFile.setServiceId(file.getKey());
						storedFile.setLibraryId(library.getId());
						storedFile.setIsOwner(true);
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
							logger.error("Error getting filename for file " + file.getValue(), e);
						}
					}

					final File systemFile = new File(storedFile.getPath());
					if (!systemFile.exists())
						storedFile.setIsDownloadComplete(false);

					try {
						storedFilesAccess.createOrUpdate(storedFile);
					} catch (SQLException e) {
						logger.error("There was an updating the stored file.", e);
					}

					return storedFile;
				} catch (SQLException e) {
					logger.error("There was an error getting access to the StoredFile table.", e);
				} finally {
					repositoryAccessHelper.close();
				}

				return null;
			}
		};

		try {
			return createOrUpdateStoredFileTask.get();
		} catch (ExecutionException | InterruptedException e) {
			logger.error("There was an error creating or updating the stored file for service file " + file.getKey(), e);
			return null;
		}
	}

	public void pruneStoredFiles(final Set<Integer> serviceIdsToKeep) {
		storedFileExecutor.execute(new Runnable() {
			@Override
			public void run() {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = repositoryAccessHelper.getDataAccess(StoredFile.class);
					// Since we could be pulling back a lot of data, only query for what we need.
					// This query is very custom to this scenario, so it's being kept here.
					final PreparedQuery<StoredFile> storedFilePreparedQuery =
							storedFileAccess
									.queryBuilder()
									.selectColumns("id", StoredFile.serviceIdColumnName, StoredFile.pathColumnName)
									.where()
									.eq(StoredFile.libraryIdColumnName, library.getId())
									.and()
									.eq(StoredFile.isOwnerColumnName, true)
									.prepare();

					final List<StoredFile> allStoredFiles = storedFileAccess.query(storedFilePreparedQuery);
					for (StoredFile storedFile : allStoredFiles) {
						if (!serviceIdsToKeep.contains(storedFile.getServiceId()))
							deleteStoredFile(storedFile);
					}
				} catch (SQLException e) {
					logger.error("Error updating the ", e);
				} finally {
					repositoryAccessHelper.close();
				}
			}
		});
	}

	private StoredFile getStoredFile(Dao<StoredFile, Integer> storedFileAccess, IFile file) {

		final PreparedQuery<StoredFile> storedFilePreparedQuery;
		try {
			storedFilePreparedQuery = storedFileAccess
					.queryBuilder()
					.where()
					.eq(StoredFile.serviceIdColumnName, file.getKey())
					.and()
					.eq(StoredFile.libraryIdColumnName, library.getId())
					.prepare();
			return storedFileAccess.queryForFirst(storedFilePreparedQuery);
		} catch (SQLException e) {
			logger.error("Error getting file!", e);
		}

		return null;
	}
}
