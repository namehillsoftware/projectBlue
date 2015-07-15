package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredFile;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import java.sql.SQLException;

/**
 * Created by david on 7/14/15.
 */
public class StoredFileProvider {

	private static final Logger mLogger = LoggerFactory.getLogger(StoredFileProvider.class);

	private final Context mContext;

	public StoredFileProvider(Context context) {
		mContext = context;
	}

	public void getStoredFile(final int storedFileId, ISimpleTask.OnCompleteListener<Void, Void, StoredFile> onStoredFileRetrieved) {
		final SimpleTask<Void, Void, StoredFile> getStoredFileTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, StoredFile>() {
			@Override
			public StoredFile onExecute(ISimpleTask<Void, Void, StoredFile> owner, Void... params) throws Exception {
				final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = dbHandler.getAccessObject(StoredFile.class);
					return storedFileAccess.queryForId(storedFileId);
				} catch (SQLException se) {
					mLogger.error("There was an error retrieving the stored file", se);
					return null;
				} finally {
					dbHandler.close();
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
				final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
				try {
					final Dao<StoredFile, Integer> storedFileAccess = dbHandler.getAccessObject(StoredFile.class);
					final StoredFile storedFile = storedFileAccess.queryForId(storedFileId);

					storedFile.setIsDownloadComplete(true);
					try {
						storedFileAccess.createOrUpdate(storedFile);
					} catch (SQLException se) {
						mLogger.error("There was an error updating the stored file", se);
					}
				} catch (SQLException se) {
					mLogger.error("There was an error retrieving the stored file", se);
				} finally {
					dbHandler.close();
				}
			}
		});
	}
}
