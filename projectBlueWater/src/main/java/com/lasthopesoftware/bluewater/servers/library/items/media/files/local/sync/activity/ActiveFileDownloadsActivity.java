package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.adapter.ActiveFileDownloadsAdapter;

/**
 * Created by david on 6/6/15.
 */
public class ActiveFileDownloadsActivity extends Activity {

    private RecyclerView activeDownloadsRecyclerView;
	private ActiveFileDownloadsAdapter activeDownloadsAdapter;
	private RecyclerView.LayoutManager layoutManager;
//	private FileDownloadProvider mFileDownloadProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_file_downloads);

	    // TODO Let's just use the file list view instead
	    activeDownloadsRecyclerView = (RecyclerView) findViewById(R.id.activeDownloadsRecyclerView);
	    activeDownloadsRecyclerView.setHasFixedSize(true);

	    layoutManager = new LinearLayoutManager(this);
	    activeDownloadsRecyclerView.setLayoutManager(layoutManager);

	    activeDownloadsAdapter = new ActiveFileDownloadsAdapter(SessionConnection.getSessionConnectionProvider(), );
	    activeDownloadsRecyclerView.setAdapter(activeDownloadsAdapter);
    }
}
