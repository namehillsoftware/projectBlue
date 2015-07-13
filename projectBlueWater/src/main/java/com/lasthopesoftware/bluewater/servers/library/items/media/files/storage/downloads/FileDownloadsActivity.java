package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.downloads;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.downloads.provider.FileDownloadProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 6/6/15.
 */
public class FileDownloadsActivity extends Activity {

    private ListView mActiveDownloadsListView;
	private FileDownloadProvider mFileDownloadProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	    mFileDownloadProvider = new FileDownloadProvider(this);

        setContentView(R.layout.activity_file_downloads);
        mActiveDownloadsListView = (ListView)findViewById(R.id.lvActiveDownloads);

	    final List<FileDownloadProvider.FileDownload> activeDownloadIds = mFileDownloadProvider.getAllActiveDownloads();
	    List<String> activedDownloadStrings = new ArrayList<>(activeDownloadIds.size());
	    for (FileDownloadProvider.FileDownload fileDownload : activeDownloadIds)
	        activedDownloadStrings.add(String.valueOf(fileDownload.id) + " - " + fileDownload.status + " (" + fileDownload.reason + ")");

        mActiveDownloadsListView.setAdapter(new ArrayAdapter<>(this, R.layout.layout_standard_text, R.id.tvStandard, activedDownloadStrings));

    }
}
