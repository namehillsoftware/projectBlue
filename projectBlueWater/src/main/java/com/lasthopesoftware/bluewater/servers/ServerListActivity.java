package com.lasthopesoftware.bluewater.servers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class ServerListActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_server);
		
		ListView serverList = (ListView) findViewById(R.id.lvServerList);
		
		serverList.setAdapter(new ServerListAdapter(LibrarySession.GetLibraries(this), LibrarySession.GetLibrary(this)));
		
		serverList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Context _context = view.getContext();
				LibrarySession.ChooseLibrary(view.getContext(), (int)id, new OnCompleteListener<Integer, Void, Library>() {

					@Override
					public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
						_context.startActivity(new Intent(_context, EditServerActivity.class));
					}
				});
				
			}
		});
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
