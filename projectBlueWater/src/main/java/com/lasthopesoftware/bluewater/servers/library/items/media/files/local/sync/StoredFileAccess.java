package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync;

import android.content.Context;
import android.net.Uri;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.stmt.PreparedQuery;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.service.StoreFilesService;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.store.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.store.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Created by david on 7/14/15.
 */
public class StoredFileAccess {

	private static final Logger mLogger = LoggerFactory.getLogger(StoredFileAccess.class);

	private final Context mContext;
	private final Library mLibrary;

	public StoredFileAccess(Context context, Library library) {
		mContext = context;
		mLibrary = library;
	}

	public void getStoredFile(final int storedFileId, ISimpleTask.OnCompleteListener<Void, Void, StoredFile> onStoredFileRetrieved) {
		final SimpleTask<Void, Void, StoredFile> getStoredFileTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, StoredFile>() {
			@Override
			public StoredFile onExecute(ISimpleTask<Void, Void, StoredFile> owner, Void... params) throws Exception {
				try {
					final Dao<StoredFile, Integer> storedFileAccess = DatabaseHandler.getInstance(mContext).getAccessObject(StoredFile.class);
					return storedFileAccess.queryForId(storedFileId);
				} catch (SQLException se) {
					mLogger.error("There was an error retrieving the stored file", se);
					return null;
				}
			}
		});

		if (onStoredFileRetrieved != null)
			getStoredFileTask.addOnCompleteListener(onStoredFileRetrieved);

		getStoredFileTask.execute(DatabaseHandler.databaseExecutor);
	}

	public void markStoredFileAsDownloaded(final int storedFileId) {
		DatabaseHandler.databaseExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final Dao<StoredFile, Integer> storedFileAccess = DatabaseHandler.getInstance(mContext).getAccessObject(StoredFile.class);
					final StoredFile storedFile = storedFileAccess.queryForId(storedFileId);

					storedFile.setIsDownloadComplete(true);
					try {
						storedFileAccess.createOrUpdate(storedFile);
					} catch (SQLException se) {
						mLogger.error("There was an error updating the stored file", se);
					}
				} catch (SQLException se) {
					mLogger.error("There was an error retrieving the stored file", se);
				}
			}
		});
	}

	public void deleteStoredFile(final StoredFile storedFile) {
		DatabaseHandler.databaseExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final Dao<StoredFile, Integer> storedFileAccess = DatabaseHandler.getInstance(mContext).getAccessObject(StoredFile.class);
					try {
						storedFileAccess.delete(storedFile);
					} catch (SQLException se) {
						mLogger.error("There was an error updating the stored file", se);
					}
				} catch (SQLException se) {
					mLogger.error("There was an error retrieving the stored file", se);
				}
			}
		});
	}

	public void syncFilesSynchronously(Dao<StoredFile, Integer> storedFilesAccess, List<IFile> files) {
		try {
			for (IFile file : files) {
				final PreparedQuery<StoredFile> storedFilePreparedQuery =
						storedFilesAccess
								.queryBuilder()
								.where()
								.eq(StoredFile.serviceIdColumnName, file.getKey())
								.and()
								.eq(StoredFile.libraryIdColumnName, mLibrary.getId())
								.prepare();

				StoredFile storedFile = storedFilesAccess.queryForFirst(storedFilePreparedQuery);
				if (storedFile == null) {
					storedFile = new StoredFile();
					storedFile.setServiceId(file.getKey());
					storedFile.setLibrary(mLibrary);
					storedFile.setIsOwner(true);
				}

				if (storedFile.getPath() == null) {
					try {
						final Uri localUri = file.getLocalFileUri(mContext);
						if (localUri != null) {
							storedFile.setPath(localUri.getPath());
							storedFile.setIsDownloadComplete(true);
							storedFile.setIsOwner(false);
						}
					} catch (IOException e) {
						mLogger.error("Error retrieving local file URI", e);
					}
				}

				if (storedFile.getPath() == null) {
					try {
						String fileName = file.getProperty(FilePropertiesProvider.FILENAME);
						fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);

						final int extensionIndex = fileName.lastIndexOf('.');
						if (extensionIndex > -1)
							fileName = fileName.substring(0, extensionIndex + 1) + "mp3";

						String fullPath = mLibrary.getSyncDir(mContext).getPath();

						String artist = file.tryGetProperty(FilePropertiesProvider.ALBUM_ARTIST);
						if (artist == null)
							artist = file.tryGetProperty(FilePropertiesProvider.ARTIST);

						if (artist != null)
							fullPath = FilenameUtils.concat(fullPath, artist);

						final String album = file.tryGetProperty(FilePropertiesProvider.ALBUM);
						if (album != null)
							fullPath = FilenameUtils.concat(fullPath, album);

						fullPath = FilenameUtils.concat(fullPath, fileName);
						storedFile.setPath(fullPath);
					} catch (IOException e) {
						mLogger.error("Error getting filename for file " + file.getValue(), e);
					}
				}

				storedFilesAccess.createOrUpdate(storedFile);

				if (!storedFile.isDownloadComplete())
					StoreFilesService.queueFileForDownload(mContext, file, storedFile);
			}
		} catch (SQLException e) {
			mLogger.error("There was an updating the stored file.", e);
		}
	}

	public void pruneStoredFiles(final Set<Integer> serviceIdsToKeep) {
		DatabaseHandler.databaseExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					final Dao<StoredFile, Integer> storedFileAccess = DatabaseHandler.getInstance(mContext).getAccessObject(StoredFile.class);
					// Since we could be pulling back a lot of data, only query for what we need.
					// This query is very custom to this scenario, so it's being kept here.
					final PreparedQuery<StoredFile> storedFilePreparedQuery =
							storedFileAccess
									.queryBuilder()
									.selectColumns("id", StoredFile.serviceIdColumnName, StoredFile.pathColumnName)
									.where()
									.eq(StoredFile.libraryIdColumnName, mLibrary.getId())
									.prepare();

					final List<StoredFile> allStoredFiles = storedFileAccess.query(storedFilePreparedQuery);
					for (StoredFile storedFile : allStoredFiles) {
						if (!serviceIdsToKeep.contains(storedFile.getServiceId()))
							deleteStoredFile(storedFile);
					}
				} catch (SQLException e) {
					mLogger.error("Error updating the ", e);
				}
			}
		});
	}
}
