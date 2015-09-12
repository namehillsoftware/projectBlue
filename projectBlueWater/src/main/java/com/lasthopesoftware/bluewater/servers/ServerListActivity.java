package com.lasthopesoftware.bluewater.servers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

public class ServerListActivity extends Activity {

	private ProgressBar mProgressBar;
	private ListView mServerListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_server);

		mServerListView = (ListView) findViewById(R.id.lvServerList);
		mProgressBar = (ProgressBar) findViewById(R.id.pbLoadingServerList);

		updateServerList();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		updateServerList();
	}

	private void updateServerList() {
		final Activity activity = this;

		mServerListView.setVisibility(View.INVISIBLE);
		mProgressBar.setVisibility(View.VISIBLE);

		LibrarySession.GetLibraries(activity, new ISimpleTask.OnCompleteListener<Void, Void, List<Library>>() {
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Library>> owner, final List<Library> libraries) {
				LibrarySession.GetActiveLibrary(activity, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
					@Override
					public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
						((ListView) findViewById(R.id.lvServerList)).setAdapter(new ServerListAdapter(activity, libraries, library));

						mProgressBar.setVisibility(View.INVISIBLE);
						mServerListView.setVisibility(View.VISIBLE);
					}
				});
			}
		});

	}
}
