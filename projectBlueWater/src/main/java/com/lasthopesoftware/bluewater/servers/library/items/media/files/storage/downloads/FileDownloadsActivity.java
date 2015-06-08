package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.downloads;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.R;

/**
 * Created by david on 6/6/15.
 */
public class FileDownloadsActivity extends Activity {

    private ListView mActiveDownloadsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_file_downloads);
        mActiveDownloadsListView = (ListView)findViewById(R.id.lvActiveDownloads);
    }
}
