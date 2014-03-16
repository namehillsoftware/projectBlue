package com.lasthopesoftware.bluewater.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.ServerListAdapter;
import com.lasthopesoftware.bluewater.data.session.JrSession;

public class WaitForConnection extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wait_for_connection);
		
		ListView serverList = (ListView) findViewById(R.id.lvServerList);
		
		serverList.setAdapter(new ServerListAdapter(this, JrSession.GetLibraries(this)));
		
		serverList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				JrSession.ChooseLibrary(view.getContext(), (int)id);
				
				view.getContext().startActivity(new Intent(view.getContext(), SetConnection.class));
			}
		});
	}
}
