package com.lasthopesoftware.bluewater.servers;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;

public class ServerListActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_server);
		
		((ListView) findViewById(R.id.lvServerList)).setAdapter(new ServerListAdapter(LibrarySession.GetLibraries(this), LibrarySession.GetLibrary(this)));
	}
}
