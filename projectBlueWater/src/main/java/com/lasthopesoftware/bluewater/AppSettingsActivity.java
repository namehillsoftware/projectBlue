package com.lasthopesoftware.bluewater;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.ServerListAdapter;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.threading.ISimpleTask;

import java.util.List;

public class AppSettingsActivity extends AppCompatActivity {
	private ProgressBar progressBar;
	private ListView serverListView;
	private CheckBox syncOnWifiCheckbox;
	private CheckBox syncOnPowerCheckbox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_server);

		serverListView = (ListView) findViewById(R.id.lvServerList);
		progressBar = (ProgressBar) findViewById(R.id.pbLoadingServerList);

		final RelativeLayout editAppSettingsView = (RelativeLayout) getLayoutInflater().inflate(R.layout.layout_edit_app_settings, null);
		serverListView.addHeaderView(editAppSettingsView);

		syncOnWifiCheckbox = (CheckBox) findViewById(R.id.syncOnWifiCheckbox);
		syncOnPowerCheckbox = (CheckBox) findViewById(R.id.syncOnPowerCheckbox);

		syncOnWifiCheckbox.setEnabled(false);
		syncOnPowerCheckbox.setEnabled(false);

		new AsyncTask<Void, Void, Void>() {
			private static final String isSyncOnWifiOnlyKey = "isSyncOnWifiOnly";
			private static final String isSyncOnPowerOnlyKey = "isSyncOnPowerOnly";

			boolean isSyncOnWifiOnly, isSyncOnPowerOnly;

			@Override
			protected Void doInBackground(Void... params) {
				final SharedPreferences sharedPreferences = AppSettingsActivity.this.getSharedPreferences(ApplicationConstants.PREFS_FILE, 0);

				isSyncOnPowerOnly = sharedPreferences.getBoolean(isSyncOnPowerOnlyKey, false);
				isSyncOnWifiOnly = sharedPreferences.getBoolean(isSyncOnWifiOnlyKey, false);

				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);

				syncOnPowerCheckbox.setChecked(isSyncOnPowerOnly);
				syncOnPowerCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						AppSettingsActivity.this
								.getSharedPreferences(ApplicationConstants.PREFS_FILE, 0).edit()
								.putBoolean(isSyncOnPowerOnlyKey, isChecked)
								.apply();
					}
				});

				syncOnWifiCheckbox.setChecked(isSyncOnWifiOnly);
				syncOnWifiCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						AppSettingsActivity.this
								.getSharedPreferences(ApplicationConstants.PREFS_FILE, 0).edit()
								.putBoolean(isSyncOnWifiOnlyKey, isChecked)
								.apply();
					}
				});

				syncOnPowerCheckbox.setEnabled(true);
				syncOnWifiCheckbox.setEnabled(true);
			}
		}.execute();

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

		LibrarySession.GetLibraries(activity, new ISimpleTask.OnCompleteListener<Void, Void, List<Library>>() {
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Library>> owner, final List<Library> libraries) {
				LibrarySession.GetActiveLibrary(activity, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {
					@Override
					public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
						((ListView) findViewById(R.id.lvServerList)).setAdapter(new ServerListAdapter(activity, libraries, library));

						progressBar.setVisibility(View.INVISIBLE);
						serverListView.setVisibility(View.VISIBLE);
					}
				});
			}
		});

	}
}
