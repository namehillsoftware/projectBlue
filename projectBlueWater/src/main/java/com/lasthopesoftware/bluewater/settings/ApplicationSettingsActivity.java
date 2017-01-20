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
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.client.servers.list.ServerListAdapter;

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
		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isVolumeLevelingEnabled, (CheckBox) findViewById(R.id.isVolumeLevelingEnabled));

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

		LibrarySession.getLibraries(activity, libraries -> LibrarySession.getActiveLibrary(activity, library -> {
			serverListView.setAdapter(new ServerListAdapter(activity, libraries, library));

			progressBar.setVisibility(View.INVISIBLE);
			serverListView.setVisibility(View.VISIBLE);
		}));
	}
}
