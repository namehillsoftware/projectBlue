package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.adapter.ActiveFileDownloadsAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.repository.Library;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

/**
 * Created by david on 6/6/15.
 */
public class ActiveFileDownloadsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);

	    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.pbLoadingFileList);
	    final ListView listView = (ListView) findViewById(R.id.lvFilelist);

	    listView.setVisibility(View.INVISIBLE);
	    progressBar.setVisibility(View.VISIBLE);

        LibrarySession.GetActiveLibrary(this, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
            @Override
            public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
                final StoredFileAccess storedFileAccess = new StoredFileAccess(ActiveFileDownloadsActivity.this, library);
                storedFileAccess.getDownloadingStoredFiles(new ISimpleTask.OnCompleteListener<Void, Void, List<StoredFile>>() {
                    @Override
                    public void onComplete(ISimpleTask<Void, Void, List<StoredFile>> owner, List<StoredFile> storedFiles) {
	                    listView.setAdapter(new ActiveFileDownloadsAdapter(ActiveFileDownloadsActivity.this, R.id.tvStandard, SessionConnection.getSessionConnectionProvider(), storedFiles));

	                    progressBar.setVisibility(View.INVISIBLE);
	                    listView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

    }
}
