package com.lasthopesoftware.bluewater.servers;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;

public class ServerListActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_server);
		
		((ListView) findViewById(R.id.lvServerList)).setAdapter(new ServerListAdapter(LibrarySession.GetLibraries(this), LibrarySession.GetLibrary(this)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		ViewUtils.buildStandardMenu(this, menu);
		menu.findItem(R.id.menu_connection_settings).setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return ViewUtils.handleMenuClicks(this, item);
	}

}
