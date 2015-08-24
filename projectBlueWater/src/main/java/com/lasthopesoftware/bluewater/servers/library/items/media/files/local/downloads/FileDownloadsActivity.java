package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.downloads;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.lasthopesoftware.bluewater.R;

/**
 * Created by david on 6/6/15.
 */
public class FileDownloadsActivity extends Activity {

    private RecyclerView activeDownloadsRecyclerView;
	private RecyclerView.Adapter activeDownloadsAdapter;
//	private FileDownloadProvider mFileDownloadProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_downloads);

	    activeDownloadsRecyclerView = (RecyclerView) findViewById(R.id.activeDownloadsRecyclerView);
	    activeDownloadsRecyclerView.setHasFixedSize(true);


//	    mFileDownloadProvider = new FileDownloadProvider(this);
//
//        setContentView(R.layout.activity_file_downloads);
//        mActiveDownloadsListView = (ListView)findViewById(R.id.lvActiveDownloads);
//
//	    final List<FileDownloadProvider.FileDownload> activeDownloadIds = mFileDownloadProvider.getAllActiveDownloads();
//	    List<String> activedDownloadStrings = new ArrayList<>(activeDownloadIds.size());
//	    for (FileDownloadProvider.FileDownload fileDownload : activeDownloadIds)
//	        activedDownloadStrings.add(String.valueOf(fileDownload.id) + " - " + fileDownload.status + " (" + fileDownload.reason + ")");
//
//        mActiveDownloadsListView.setAdapter(new ArrayAdapter<>(this, R.layout.layout_standard_text, R.id.tvStandard, activedDownloadStrings));

    }
}
