package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.adapter.ActiveFileDownloadsAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.sync.service.ItemSyncService;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

/**
 * Created by david on 6/6/15.
 */
public class ActiveFileDownloadsActivity extends AppCompatActivity {

    private BroadcastReceiver onFileDownloadedReceiver;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);

	    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.pbLoadingFileList);
	    final ListView listView = (ListView) findViewById(R.id.lvFilelist);

	    listView.setVisibility(View.INVISIBLE);
	    progressBar.setVisibility(View.VISIBLE);

        localBroadcastManager = LocalBroadcastManager.getInstance(ActiveFileDownloadsActivity.this);

        LibrarySession.GetActiveLibrary(this, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
            @Override
            public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
                final StoredFileAccess storedFileAccess = new StoredFileAccess(ActiveFileDownloadsActivity.this, library);
                storedFileAccess.getDownloadingStoredFiles(new ISimpleTask.OnCompleteListener<Void, Void, List<StoredFile>>() {
                    @Override
                    public void onComplete(ISimpleTask<Void, Void, List<StoredFile>> owner, final List<StoredFile> storedFiles) {
                        final ActiveFileDownloadsAdapter activeFileDownloadsAdapter = new ActiveFileDownloadsAdapter(ActiveFileDownloadsActivity.this, R.id.tvStandard, SessionConnection.getSessionConnectionProvider(), storedFiles);

                        if (onFileDownloadedReceiver != null)
                            localBroadcastManager.unregisterReceiver(onFileDownloadedReceiver);

                        onFileDownloadedReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                final int storedFileId = intent.getIntExtra(ItemSyncService.onFileDownloadedStoreId, -1);

	                            final List<IFile> files = activeFileDownloadsAdapter.getFiles();

                                for (StoredFile storedFile : storedFiles) {
                                    if (storedFile.getId() != storedFileId) continue;

                                    for (IFile file : files) {
                                        if (file.getKey() != storedFile.getServiceId()) continue;

                                        activeFileDownloadsAdapter.remove(file);
	                                    files.remove(file);
                                        break;
                                    }

                                    break;
                                }
                            }
                        };

                        localBroadcastManager.registerReceiver(onFileDownloadedReceiver, new IntentFilter(ItemSyncService.onFileDownloadedEvent));
	                    listView.setAdapter(activeFileDownloadsAdapter);

	                    progressBar.setVisibility(View.INVISIBLE);
	                    listView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (localBroadcastManager != null && onFileDownloadedReceiver != null)
            localBroadcastManager.unregisterReceiver(onFileDownloadedReceiver);
    }
}
