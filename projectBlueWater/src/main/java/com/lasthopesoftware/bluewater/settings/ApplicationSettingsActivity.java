package com.lasthopesoftware.bluewater.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.servers.list.ServerListAdapter;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.List;

public class ApplicationSettingsActivity extends AppCompatActivity {
	private ProgressBar progressBar;
	private ListView serverListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_server);

		serverListView = (ListView) findViewById(R.id.lvServerList);
		progressBar = (ProgressBar) findViewById(R.id.pbLoadingServerList);

		final RelativeLayout editAppSettingsView = (RelativeLayout) getLayoutInflater().inflate(R.layout.layout_edit_app_settings, serverListView, false);
		serverListView.addHeaderView(editAppSettingsView);

		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, (CheckBox) findViewById(R.id.syncOnPowerCheckbox));
		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, (CheckBox) findViewById(R.id.syncOnWifiCheckbox));

		updateServerList();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		updateServerList();
	}

	private void updateServerList() {
		final Activity activity = this;

		serverListView.setVisibility(View.INVISIBLE);
		progressBar.setVisibility(View.VISIBLE);

		LibrarySession.GetLibraries(activity, new TwoParameterRunnable<FluentTask<Void,Void,List<Library>>, List<Library>>() {
			@Override
			public void run(FluentTask<Void, Void, List<Library>> owner, final List<Library> libraries) {
				LibrarySession.GetActiveLibrary(activity, new TwoParameterRunnable<FluentTask<Integer,Void,Library>, Library>() {
					@Override
					public void run(FluentTask<Integer, Void, Library> owner, Library library) {
						((ListView) findViewById(R.id.lvServerList)).setAdapter(new ServerListAdapter(activity, libraries, library));

						progressBar.setVisibility(View.INVISIBLE);
						serverListView.setVisibility(View.VISIBLE);
					}
				});
			}
		});

	}
}
