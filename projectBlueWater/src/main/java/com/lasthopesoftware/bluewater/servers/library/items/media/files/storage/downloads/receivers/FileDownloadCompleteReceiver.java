package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.downloads.receivers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredFile;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by david on 6/7/15.
 */
public class FileDownloadCompleteReceiver extends BroadcastReceiver {

    private final static String fullClassName = FileDownloadCompleteReceiver.class.getCanonicalName();

    public final static String FILE_COMPLETE_BROADCAST = fullClassName + "FILE_COMPLETE_BROADCAST";
    public final static String FILE_COMPLETE_ID = fullClassName + "FILE_COMPLETE_ID";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        if (downloadId == -1) return;

        SimpleTask.executeNew(DatabaseHandler.databaseExecutor, new ISimpleTask.OnExecuteListener<Void, Void, Void>() {
            @Override
            public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
                final DatabaseHandler databaseHandler = new DatabaseHandler(context);
                try {
                    final Dao<StoredFile, Integer> storedFileAccess = databaseHandler.getAccessObject(StoredFile.class);
                    final List<StoredFile> storedFileResults = storedFileAccess.queryForEq(StoredFile.downloadIdName, downloadId);
                    if (storedFileResults.isEmpty()) return null;

                    final StoredFile storedFile = storedFileResults.get(0);
                    storedFile.setIsDownloadComplete(true);
                    storedFile.setDownloadId(-1);
                    storedFileAccess.update(storedFile);
                } catch (SQLException e) {
                    LoggerFactory.getLogger(getClass()).error("Error getting access to StoredFile table", e);
                } finally {
                    databaseHandler.close();
                }

                return null;
            }
        });
    }
}
